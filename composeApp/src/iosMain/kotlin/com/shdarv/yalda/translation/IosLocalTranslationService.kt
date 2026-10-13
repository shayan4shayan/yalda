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

interface IosStringListCallback {
    fun complete(values: List<String>, errorMessage: String?)
}

interface IosNativeTranslationBridge {
    val isSupported: Boolean
    fun downloadedLanguagePacks(callback: IosStringListCallback)
    fun downloadPair(
        sourceLanguage: String,
        targetLanguage: String,
        wifiOnly: Boolean,
        callback: IosBooleanCallback
    )
    fun cancelPairDownload(sourceLanguage: String, targetLanguage: String)
    fun deleteLanguagePack(languageCode: String, callback: IosBooleanCallback)
    fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        callback: IosStringCallback
    )
}

object IosNativeTranslations {
    private var bridge: IosNativeTranslationBridge? = null
    fun register(bridge: IosNativeTranslationBridge) { this.bridge = bridge }
    internal fun currentBridge(): IosNativeTranslationBridge? = bridge
}

class IosLocalTranslationService : LocalTranslationService {
    private val cache = linkedMapOf<String, String>()

    override val isSupported: Boolean
        get() = IosNativeTranslations.currentBridge()?.isSupported == true

    override suspend fun downloadedLanguagePacks(): Set<String> {
        val bridge = IosNativeTranslations.currentBridge() ?: return emptySet()
        return suspendCoroutine { continuation ->
            bridge.downloadedLanguagePacks(object : IosStringListCallback {
                override fun complete(values: List<String>, errorMessage: String?) {
                    continuation.resume(if (errorMessage == null) values.toSet() else emptySet())
                }
            })
        }
    }

    override suspend fun downloadPair(
        pair: TranslationPair,
        wifiOnly: Boolean,
        onProgress: (TranslationDownloadProgress) -> Unit
    ): TranslationDownloadResult {
        val bridge = IosNativeTranslations.currentBridge()
            ?: return TranslationDownloadResult.Failure("Offline translation is not available on this platform.")
        onProgress(TranslationDownloadProgress(message = "Downloading ${pair.displayName} language packs..."))
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                bridge.cancelPairDownload(pair.sourceLanguage, pair.targetLanguage)
            }
            bridge.downloadPair(
                pair.sourceLanguage,
                pair.targetLanguage,
                wifiOnly,
                object : IosBooleanCallback {
                    override fun complete(value: Boolean, errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(
                                if (value) TranslationDownloadResult.Success("${pair.displayName} is ready offline.")
                                else TranslationDownloadResult.Failure(errorMessage ?: "Translation download failed.")
                            )
                        }
                    }
                }
            )
        }
    }

    override fun cancelPairDownload(pair: TranslationPair) {
        IosNativeTranslations.currentBridge()?.cancelPairDownload(pair.sourceLanguage, pair.targetLanguage)
    }

    override suspend fun deleteLanguagePack(languageCode: String): TranslationDownloadResult {
        val bridge = IosNativeTranslations.currentBridge()
            ?: return TranslationDownloadResult.Failure("Offline translation is not available on this platform.")
        return suspendCoroutine { continuation ->
            bridge.deleteLanguagePack(languageCode, object : IosBooleanCallback {
                override fun complete(value: Boolean, errorMessage: String?) {
                    continuation.resume(
                        if (value) TranslationDownloadResult.Success(
                            "${TranslationCatalog.languageName(languageCode)} language pack was removed."
                        ) else TranslationDownloadResult.Failure(errorMessage ?: "Language pack delete failed.")
                    )
                }
            })
        }
    }

    override suspend fun translate(text: String, pair: TranslationPair): TranslationResult {
        val bridge = IosNativeTranslations.currentBridge()
            ?: return TranslationResult.Failure("Offline translation is not available on this platform.")
        if (!isPairReady(pair)) return TranslationResult.PairNotReady(pair)
        val normalized = text.trim()
        val key = "${pair.id}|${normalized.lowercase()}"
        cache[key]?.let { return TranslationResult.Success(it) }
        return suspendCoroutine { continuation ->
            bridge.translate(
                normalized,
                pair.sourceLanguage,
                pair.targetLanguage,
                object : IosStringCallback {
                    override fun complete(value: String?, errorMessage: String?) {
                        continuation.resume(
                            if (errorMessage == null && value != null) {
                                cache[key] = value
                                while (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
                                TranslationResult.Success(value)
                            } else TranslationResult.Failure(errorMessage ?: "Translation failed.")
                        )
                    }
                }
            )
        }
    }

    private companion object { const val CACHE_SIZE = 100 }
}
