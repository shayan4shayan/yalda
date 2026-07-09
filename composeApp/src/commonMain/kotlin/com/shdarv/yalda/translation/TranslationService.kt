package com.shdarv.yalda.translation

import kotlinx.serialization.Serializable

@Serializable
data class TranslationModelInfo(
    val id: String,
    val name: String,
    val sizeMb: Int,
    val runtime: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val downloadUrl: String? = null,
    val artifactFileName: String? = null
) {
    fun supports(sourceLanguage: String, targetLanguage: String): Boolean =
        this.sourceLanguage.equals(sourceLanguage, ignoreCase = true) &&
            this.targetLanguage.equals(targetLanguage, ignoreCase = true)
}

data class TranslationModelState(
    val model: TranslationModelInfo,
    val isDownloaded: Boolean = false,
    val isBusy: Boolean = false,
    val downloadProgress: Float? = null,
    val status: String? = null
)

data class TranslationDownloadProgress(
    val progress: Float? = null,
    val message: String = "Downloading..."
)

sealed interface TranslationResult {
    data class Success(val text: String) : TranslationResult
    data class ModelNotDownloaded(val model: TranslationModelInfo) : TranslationResult
    data class UnsupportedLanguagePair(
        val sourceLanguage: String,
        val targetLanguage: String
    ) : TranslationResult
    data class Failure(val message: String) : TranslationResult
}

sealed interface TranslationDownloadResult {
    data class Success(val model: TranslationModelInfo) : TranslationDownloadResult
    data object Canceled : TranslationDownloadResult
    data class Failure(val message: String) : TranslationDownloadResult
}

interface LocalTranslationService {
    val isSupported: Boolean

    fun availableModels(): List<TranslationModelInfo>

    suspend fun isModelDownloaded(modelId: String): Boolean

    suspend fun downloadModel(
        modelId: String,
        onProgress: (TranslationDownloadProgress) -> Unit = {}
    ): TranslationDownloadResult

    fun cancelModelDownload(modelId: String) = Unit

    suspend fun deleteModel(modelId: String): TranslationDownloadResult

    suspend fun translate(text: String, modelId: String): TranslationResult

    fun modelFor(sourceLanguage: String, targetLanguage: String): TranslationModelInfo? =
        availableModels().firstOrNull { it.supports(sourceLanguage, targetLanguage) }
}

object TranslationCatalog {
    const val EN_FA_MODEL_ID = "mlkit_en_fa"

    val defaultEnFaModel = TranslationModelInfo(
        id = EN_FA_MODEL_ID,
        name = "ML Kit English to Persian",
        sizeMb = 30,
        runtime = "mlkit",
        sourceLanguage = "en",
        targetLanguage = "fa"
    )
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

    override fun availableModels(): List<TranslationModelInfo> =
        listOf(TranslationCatalog.defaultEnFaModel)

    override suspend fun isModelDownloaded(modelId: String): Boolean = false

    override suspend fun downloadModel(
        modelId: String,
        onProgress: (TranslationDownloadProgress) -> Unit
    ): TranslationDownloadResult =
        TranslationDownloadResult.Failure("Offline translation is not available on this platform.")

    override suspend fun deleteModel(modelId: String): TranslationDownloadResult =
        TranslationDownloadResult.Failure("Offline translation is not available on this platform.")

    override suspend fun translate(text: String, modelId: String): TranslationResult =
        TranslationResult.Failure("Offline translation is not available on this platform.")
}
