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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import java.util.Locale

class MlKitLocalTranslationService(@Suppress("UNUSED_PARAMETER") context: Context) : LocalTranslationService {
    private val modelManager = RemoteModelManager.getInstance()
    private val translators = LinkedHashMap<String, Translator>(TRANSLATOR_CACHE_SIZE, 0.75f, true)
    private val translationCache = object : LinkedHashMap<String, String>(RESULT_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > RESULT_CACHE_SIZE
    }

    override val isSupported: Boolean = true

    override suspend fun downloadedLanguagePacks(): Set<String> = withContext(Dispatchers.IO) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await().map { it.language }.toSet()
    }

    override suspend fun downloadPair(
        pair: TranslationPair,
        wifiOnly: Boolean,
        onProgress: (TranslationDownloadProgress) -> Unit
    ): TranslationDownloadResult = withContext(Dispatchers.IO) {
        if (translatorOptions(pair) == null) {
            return@withContext TranslationDownloadResult.Failure("Unsupported translation language pair.")
        }
        try {
            onProgress(TranslationDownloadProgress(message = "Downloading ${pair.displayName} language packs..."))
            val conditions = DownloadConditions.Builder().apply {
                if (wifiOnly) requireWifi()
            }.build()
            // ML Kit owns the dependency decision, including any English intermediary models.
            translatorFor(pair).downloadModelIfNeeded(conditions).await()
            TranslationDownloadResult.Success("${pair.displayName} is ready offline.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            TranslationDownloadResult.Failure(exception.readableMessage("Translation download failed."))
        }
    }

    override fun cancelPairDownload(pair: TranslationPair) = Unit

    override suspend fun deleteLanguagePack(languageCode: String): TranslationDownloadResult =
        withContext(Dispatchers.IO) {
            val language = mlKitLanguage(languageCode)
                ?: return@withContext TranslationDownloadResult.Failure("Unsupported language pack.")
            try {
                closeTranslatorsUsing(languageCode)
                modelManager.deleteDownloadedModel(TranslateRemoteModel.Builder(language).build()).await()
                TranslationDownloadResult.Success(
                    "${TranslationCatalog.languageName(languageCode)} language pack was removed."
                )
            } catch (exception: Exception) {
                TranslationDownloadResult.Failure(exception.readableMessage("Language pack delete failed."))
            }
        }

    override suspend fun translate(text: String, pair: TranslationPair): TranslationResult =
        withContext(Dispatchers.IO) {
            if (!isPairReady(pair)) return@withContext TranslationResult.PairNotReady(pair)
            val normalizedInput = text.trim()
            val cacheKey = "${pair.id}|${normalizedInput.lowercase(Locale.ROOT)}"
            synchronized(translationCache) {
                translationCache[cacheKey]?.let { return@withContext TranslationResult.Success(it) }
            }
            try {
                val translated = translatorFor(pair).translate(normalizedInput).await()
                synchronized(translationCache) { translationCache[cacheKey] = translated }
                TranslationResult.Success(translated)
            } catch (exception: Exception) {
                TranslationResult.Failure(exception.readableMessage("Translation failed."))
            }
        }

    @Synchronized
    private fun translatorFor(pair: TranslationPair): Translator {
        translators[pair.id]?.let { return it }
        val options = translatorOptions(pair) ?: error("Unsupported translation language pair.")
        val translator = Translation.getClient(options)
        translators[pair.id] = translator
        while (translators.size > TRANSLATOR_CACHE_SIZE) {
            val oldest = translators.entries.first()
            oldest.value.close()
            translators.remove(oldest.key)
        }
        return translator
    }

    @Synchronized
    private fun closeTranslatorsUsing(languageCode: String) {
        val pairIds = TranslationCatalog.directedPairs
            .filter { languageCode in it.requiredLanguagePacks }
            .map(TranslationPair::id)
            .toSet()
        translators.entries.removeAll { entry ->
            if (entry.key in pairIds) entry.value.close()
            entry.key in pairIds
        }
        translationCache.keys.removeAll { key -> pairIds.any { pairId -> key.startsWith("$pairId|") } }
    }

    private fun translatorOptions(pair: TranslationPair): TranslatorOptions? {
        val source = mlKitLanguage(pair.sourceLanguage) ?: return null
        val target = mlKitLanguage(pair.targetLanguage) ?: return null
        return TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
    }

    private fun mlKitLanguage(languageCode: String): String? =
        TranslateLanguage.fromLanguageTag(languageCode)

    private fun Exception.readableMessage(fallback: String): String =
        localizedMessage?.takeIf(String::isNotBlank) ?: fallback

    private companion object {
        const val RESULT_CACHE_SIZE = 100
        const val TRANSLATOR_CACHE_SIZE = 8
    }
}
