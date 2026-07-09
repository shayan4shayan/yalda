package com.shdarv.yalda.translation

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface IosBooleanCallback {
    fun complete(value: Boolean, errorMessage: String?)
}

interface IosStringCallback {
    fun complete(value: String?, errorMessage: String?)
}

interface IosNativeTranslationBridge {
    val isSupported: Boolean

    fun isModelDownloaded(modelId: String, callback: IosBooleanCallback)

    fun downloadModel(modelId: String, callback: IosBooleanCallback)

    fun cancelModelDownload(modelId: String)

    fun deleteModel(modelId: String, callback: IosBooleanCallback)

    fun translate(text: String, modelId: String, callback: IosStringCallback)
}

object IosNativeTranslations {
    private var bridge: IosNativeTranslationBridge? = null

    fun register(bridge: IosNativeTranslationBridge) {
        this.bridge = bridge
    }

    internal fun currentBridge(): IosNativeTranslationBridge? = bridge
}

class IosLocalTranslationService : LocalTranslationService {
    private val cache = mutableMapOf<String, String>()
    private val cacheKeys = mutableListOf<String>()

    override val isSupported: Boolean
        get() = IosNativeTranslations.currentBridge()?.isSupported == true

    override fun availableModels(): List<TranslationModelInfo> =
        listOf(TranslationCatalog.defaultEnFaModel)

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        val bridge = IosNativeTranslations.currentBridge() ?: return false
        if (modelId != TranslationCatalog.EN_FA_MODEL_ID) return false

        return suspendCoroutine { continuation ->
            bridge.isModelDownloaded(
                modelId,
                object : IosBooleanCallback {
                    override fun complete(value: Boolean, errorMessage: String?) {
                        continuation.resume(errorMessage == null && value)
                    }
                }
            )
        }
    }

    override suspend fun downloadModel(
        modelId: String,
        onProgress: (TranslationDownloadProgress) -> Unit
    ): TranslationDownloadResult {
        val model = availableModels().firstOrNull { it.id == modelId }
            ?: return TranslationDownloadResult.Failure("Translation model was not found.")
        val bridge = IosNativeTranslations.currentBridge()
            ?: return TranslationDownloadResult.Failure("Offline translation is not available on this platform.")

        onProgress(TranslationDownloadProgress(message = "Downloading model..."))

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                bridge.cancelModelDownload(modelId)
            }
            bridge.downloadModel(
                modelId,
                object : IosBooleanCallback {
                    override fun complete(value: Boolean, errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(
                                if (value) {
                                    TranslationDownloadResult.Success(model)
                                } else {
                                    TranslationDownloadResult.Failure(errorMessage ?: "Model download failed.")
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    override fun cancelModelDownload(modelId: String) {
        IosNativeTranslations.currentBridge()?.cancelModelDownload(modelId)
    }

    override suspend fun deleteModel(modelId: String): TranslationDownloadResult {
        val model = availableModels().firstOrNull { it.id == modelId }
            ?: return TranslationDownloadResult.Failure("Translation model was not found.")
        val bridge = IosNativeTranslations.currentBridge()
            ?: return TranslationDownloadResult.Failure("Offline translation is not available on this platform.")

        return suspendCoroutine { continuation ->
            bridge.deleteModel(
                modelId,
                object : IosBooleanCallback {
                    override fun complete(value: Boolean, errorMessage: String?) {
                        continuation.resume(
                            if (value) {
                                TranslationDownloadResult.Success(model)
                            } else {
                                TranslationDownloadResult.Failure(errorMessage ?: "Model delete failed.")
                            }
                        )
                    }
                }
            )
        }
    }

    override suspend fun translate(text: String, modelId: String): TranslationResult {
        val model = availableModels().firstOrNull { it.id == modelId }
            ?: return TranslationResult.Failure("Translation model was not found.")
        val bridge = IosNativeTranslations.currentBridge()
            ?: return TranslationResult.Failure("Offline translation is not available on this platform.")

        if (!isModelDownloaded(modelId)) {
            return TranslationResult.ModelNotDownloaded(model)
        }

        val normalizedInput = text.trim()
        val cacheKey = "${model.runtime}|${model.id}|${model.sourceLanguage}|${model.targetLanguage}|${normalizedInput.lowercase()}"
        cachedTranslation(cacheKey)?.let { return TranslationResult.Success(it) }

        return suspendCoroutine { continuation ->
            bridge.translate(
                normalizedInput,
                modelId,
                object : IosStringCallback {
                    override fun complete(value: String?, errorMessage: String?) {
                        val translated = value
                        continuation.resume(
                            if (errorMessage == null && translated != null) {
                                cacheTranslation(cacheKey, translated)
                                TranslationResult.Success(translated)
                            } else {
                                TranslationResult.Failure(errorMessage ?: "Translation failed.")
                            }
                        )
                    }
                }
            )
        }
    }

    private companion object {
        const val CACHE_SIZE = 100
    }

    private fun cachedTranslation(cacheKey: String): String? {
        val value = cache[cacheKey] ?: return null
        cacheKeys.remove(cacheKey)
        cacheKeys.add(cacheKey)
        return value
    }

    private fun cacheTranslation(cacheKey: String, translated: String) {
        if (cache.containsKey(cacheKey)) {
            cacheKeys.remove(cacheKey)
        }
        cache[cacheKey] = translated
        cacheKeys.add(cacheKey)

        while (cacheKeys.size > CACHE_SIZE) {
            val oldestKey = cacheKeys.removeAt(0)
            cache.remove(oldestKey)
        }
    }
}
