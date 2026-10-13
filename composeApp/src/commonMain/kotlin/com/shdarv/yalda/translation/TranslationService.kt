package com.shdarv.yalda.translation

data class TranslationLanguage(
    val code: String,
    val name: String
)

data class TranslationPair(
    val sourceLanguage: String,
    val targetLanguage: String
) {
    init {
        require(sourceLanguage != targetLanguage) { "Translation languages must differ." }
    }

    val id: String = "mlkit_${sourceLanguage}_$targetLanguage"
    val requiredLanguagePacks: Set<String> = linkedSetOf(sourceLanguage, targetLanguage)
    val estimatedSizeMb: Int = requiredLanguagePacks.size * TranslationCatalog.ESTIMATED_PACK_SIZE_MB
    val displayName: String
        get() = "${TranslationCatalog.languageName(sourceLanguage)} to ${TranslationCatalog.languageName(targetLanguage)}"
}

data class TranslationLanguagePackState(
    val language: TranslationLanguage,
    val isDownloaded: Boolean = false,
    val isBusy: Boolean = false,
    val status: String? = null
)

data class TranslationDownloadProgress(
    val progress: Float? = null,
    val message: String = "Downloading language packs..."
)

sealed interface TranslationResult {
    data class Success(val text: String) : TranslationResult
    data class PairNotReady(val pair: TranslationPair) : TranslationResult
    data class UnsupportedLanguagePair(
        val sourceLanguage: String,
        val targetLanguage: String
    ) : TranslationResult
    data class Failure(val message: String) : TranslationResult
}

sealed interface TranslationDownloadResult {
    data class Success(val message: String) : TranslationDownloadResult
    data object Canceled : TranslationDownloadResult
    data class Failure(val message: String) : TranslationDownloadResult
}

interface LocalTranslationService {
    val isSupported: Boolean

    fun availableLanguages(): List<TranslationLanguage> = TranslationCatalog.languages

    suspend fun downloadedLanguagePacks(): Set<String>

    suspend fun isPairReady(pair: TranslationPair): Boolean =
        translationPairIsReady(pair, downloadedLanguagePacks())

    suspend fun downloadPair(
        pair: TranslationPair,
        wifiOnly: Boolean = true,
        onProgress: (TranslationDownloadProgress) -> Unit = {}
    ): TranslationDownloadResult

    fun cancelPairDownload(pair: TranslationPair) = Unit

    suspend fun deleteLanguagePack(languageCode: String): TranslationDownloadResult

    suspend fun translate(text: String, pair: TranslationPair): TranslationResult
}

fun translationPairIsReady(pair: TranslationPair, downloadedLanguagePacks: Set<String>): Boolean =
    downloadedLanguagePacks.containsAll(pair.requiredLanguagePacks)

object TranslationCatalog {
    const val ESTIMATED_PACK_SIZE_MB = 30

    val languages = listOf(
        TranslationLanguage("en", "English"),
        TranslationLanguage("es", "Spanish"),
        TranslationLanguage("fr", "French"),
        TranslationLanguage("de", "German"),
        TranslationLanguage("ja", "Japanese"),
        TranslationLanguage("fa", "Persian"),
        TranslationLanguage("it", "Italian"),
        TranslationLanguage("pt", "Portuguese")
    )

    val directedPairs: List<TranslationPair> = languages.flatMap { source ->
        languages.filter { it.code != source.code }.map { target ->
            TranslationPair(source.code, target.code)
        }
    }

    fun pairFor(sourceLanguage: String, targetLanguage: String): TranslationPair? =
        directedPairs.firstOrNull {
            it.sourceLanguage.equals(sourceLanguage, ignoreCase = true) &&
                it.targetLanguage.equals(targetLanguage, ignoreCase = true)
        }

    fun languageName(code: String): String =
        languages.firstOrNull { it.code == code }?.name ?: code.uppercase()
}

object localTranslations {
    private var service: LocalTranslationService = UnsupportedTranslationService()

    fun init(localTranslationService: LocalTranslationService) {
        service = localTranslationService
    }

    fun get(): LocalTranslationService = service
}

private class UnsupportedTranslationService : LocalTranslationService {
    override val isSupported: Boolean = false
    override suspend fun downloadedLanguagePacks(): Set<String> = emptySet()
    override suspend fun downloadPair(
        pair: TranslationPair,
        wifiOnly: Boolean,
        onProgress: (TranslationDownloadProgress) -> Unit
    ): TranslationDownloadResult = TranslationDownloadResult.Failure(
        "Offline translation is not available on this platform."
    )
    override suspend fun deleteLanguagePack(languageCode: String): TranslationDownloadResult =
        TranslationDownloadResult.Failure("Offline translation is not available on this platform.")
    override suspend fun translate(text: String, pair: TranslationPair): TranslationResult =
        TranslationResult.Failure("Offline translation is not available on this platform.")
}
