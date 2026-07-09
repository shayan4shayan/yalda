package com.shdarv.yalda.translation

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.LinkedHashMap
import java.util.Locale

class MlKitLocalTranslationService(context: Context) : LocalTranslationService {
    private val appContext = context.applicationContext
    private val modelManager = RemoteModelManager.getInstance()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_SIZE
    }

    private var activeModelId: String? = null
    private var activeTranslator: Translator? = null

    override val isSupported: Boolean = true

    private val models: List<TranslationModelInfo> by lazy {
        loadModelMetadata().filter { model ->
            model.runtime == MLKIT_RUNTIME &&
                model.sourceLanguage == ENGLISH &&
                model.targetLanguage == PERSIAN
        }
    }

    override fun availableModels(): List<TranslationModelInfo> = models

    override suspend fun isModelDownloaded(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val model = models.firstOrNull { it.id == modelId } ?: return@withContext false
        val targetLanguage = mlKitLanguage(model.targetLanguage) ?: return@withContext false
        val downloadedModels = modelManager
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()

        downloadedModels.any { it.language == targetLanguage }
    }

    override suspend fun downloadModel(
        modelId: String,
        onProgress: (TranslationDownloadProgress) -> Unit
    ): TranslationDownloadResult =
        withContext(Dispatchers.IO) {
            val model = models.firstOrNull { it.id == modelId }
                ?: return@withContext TranslationDownloadResult.Failure("Translation model was not found.")
            val remoteModel = remoteModelFor(model)
                ?: return@withContext TranslationDownloadResult.Failure("Unsupported translation language pair.")

            try {
                if (modelManager.isModelDownloaded(remoteModel).await()) {
                    return@withContext TranslationDownloadResult.Success(model)
                }

                onProgress(TranslationDownloadProgress(message = "Downloading model..."))
                val downloadTask = modelManager.download(remoteModel, DownloadConditions.Builder().build())
                val result = withTimeoutOrNull<TranslationDownloadResult>(DOWNLOAD_TIMEOUT_MS) {
                    while (!downloadTask.isComplete) {
                        if (modelManager.isModelDownloaded(remoteModel).await()) {
                            return@withTimeoutOrNull TranslationDownloadResult.Success(model)
                        }
                        delay(DOWNLOAD_POLL_INTERVAL_MS)
                    }

                    downloadTask.await()
                    if (modelManager.isModelDownloaded(remoteModel).await()) {
                        TranslationDownloadResult.Success(model)
                    } else {
                        TranslationDownloadResult.Failure("Model download finished, but the model is not ready yet. Try again.")
                    }
                }

                result ?: if (modelManager.isModelDownloaded(remoteModel).await()) {
                    TranslationDownloadResult.Success(model)
                } else {
                    TranslationDownloadResult.Failure("Model download timed out. Check your connection and try again.")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                TranslationDownloadResult.Failure(exception.readableMessage("Model download failed."))
            }
        }

    override fun cancelModelDownload(modelId: String) = Unit

    override suspend fun deleteModel(modelId: String): TranslationDownloadResult =
        withContext(Dispatchers.IO) {
            val model = models.firstOrNull { it.id == modelId }
                ?: return@withContext TranslationDownloadResult.Failure("Translation model was not found.")
            val remoteModel = remoteModelFor(model)
                ?: return@withContext TranslationDownloadResult.Failure("Unsupported translation language pair.")

            try {
                closeActiveTranslator()
                modelManager.deleteDownloadedModel(remoteModel).await()
                TranslationDownloadResult.Success(model)
            } catch (exception: Exception) {
                TranslationDownloadResult.Failure(exception.readableMessage("Model delete failed."))
            }
        }

    override suspend fun translate(text: String, modelId: String): TranslationResult =
        withContext(Dispatchers.IO) {
            val model = models.firstOrNull { it.id == modelId }
                ?: return@withContext TranslationResult.Failure("Translation model was not found.")
            if (!isModelDownloaded(modelId)) {
                return@withContext TranslationResult.ModelNotDownloaded(model)
            }

            val normalizedInput = text.trim()
            val cacheKey = cacheKey(model, normalizedInput)
            synchronized(cache) {
                cache[cacheKey]?.let { return@withContext TranslationResult.Success(it) }
            }

            try {
                val translated = translatorFor(model).translate(normalizedInput).await()
                synchronized(cache) {
                    cache[cacheKey] = translated
                }
                TranslationResult.Success(translated)
            } catch (exception: Exception) {
                TranslationResult.Failure(exception.readableMessage("Translation failed."))
            }
        }

    private fun loadModelMetadata(): List<TranslationModelInfo> =
        runCatching {
            appContext.assets.open(METADATA_ASSET).bufferedReader().use { reader ->
                json.decodeFromString<List<TranslationModelInfo>>(reader.readText())
            }
        }.getOrElse {
            listOf(TranslationCatalog.defaultEnFaModel)
        }

    @Synchronized
    private fun translatorFor(model: TranslationModelInfo): Translator {
        if (activeModelId != model.id || activeTranslator == null) {
            activeTranslator?.close()
            activeTranslator = Translation.getClient(
                translatorOptions(model) ?: error("Unsupported translation language pair.")
            )
            activeModelId = model.id
        }
        return activeTranslator ?: error("Translator was not initialized.")
    }

    @Synchronized
    private fun closeActiveTranslator() {
        activeTranslator?.close()
        activeTranslator = null
        activeModelId = null
    }

    private fun translatorOptions(model: TranslationModelInfo): TranslatorOptions? {
        val sourceLanguage = mlKitLanguage(model.sourceLanguage) ?: return null
        val targetLanguage = mlKitLanguage(model.targetLanguage) ?: return null
        return TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
    }

    private fun mlKitLanguage(languageCode: String): String? =
        TranslateLanguage.fromLanguageTag(languageCode)

    private fun remoteModelFor(model: TranslationModelInfo): TranslateRemoteModel? {
        val targetLanguage = mlKitLanguage(model.targetLanguage) ?: return null
        return TranslateRemoteModel.Builder(targetLanguage).build()
    }

    private fun cacheKey(model: TranslationModelInfo, input: String): String =
        listOf(
            model.runtime,
            model.id,
            model.sourceLanguage,
            model.targetLanguage,
            input.lowercase(Locale.ROOT)
        ).joinToString(separator = "|")

    private fun Exception.readableMessage(fallback: String): String =
        localizedMessage?.takeIf { it.isNotBlank() } ?: fallback

    private companion object {
        const val CACHE_SIZE = 100
        const val METADATA_ASSET = "model_metadata.json"
        const val MLKIT_RUNTIME = "mlkit"
        const val ENGLISH = "en"
        const val PERSIAN = "fa"
        const val DOWNLOAD_POLL_INTERVAL_MS = 1_000L
        const val DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1_000L
    }
}
