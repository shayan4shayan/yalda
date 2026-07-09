package com.shdarv.yalda.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationUiState(
    val isSupported: Boolean = false,
    val models: List<TranslationModelState> = emptyList(),
    val isRefreshing: Boolean = false,
    val isTranslating: Boolean = false,
    val message: String? = null
)

class TranslationViewModel(
    private val service: LocalTranslationService = localTranslations.get()
) : ViewModel() {
    private val downloadJobs = mutableMapOf<String, Job>()

    private val _uiState = MutableStateFlow(
        TranslationUiState(
            isSupported = service.isSupported,
            models = service.availableModels().map { TranslationModelState(model = it) }
        )
    )
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshModels()
        }
    }

    fun downloadModel(modelId: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (downloadJobs[modelId]?.isActive == true) return

        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            var success = false
            try {
                setModelDownloadState(
                    modelId = modelId,
                    isBusy = true,
                    progress = null,
                    status = "Starting download..."
                )

                val result = service.downloadModel(modelId) { progress ->
                    setModelDownloadState(
                        modelId = modelId,
                        isBusy = true,
                        progress = progress.progress,
                        status = progress.message
                    )
                }
                success = result is TranslationDownloadResult.Success

                refreshModels(
                    message = when (result) {
                        is TranslationDownloadResult.Success -> "${result.model.name} is ready."
                        TranslationDownloadResult.Canceled -> "Model download canceled."
                        is TranslationDownloadResult.Failure -> result.message
                    }
                )
            } catch (exception: CancellationException) {
                service.cancelModelDownload(modelId)
                setMessage("Model download canceled.")
            } finally {
                downloadJobs.remove(modelId)
                setModelDownloadState(
                    modelId = modelId,
                    isBusy = false,
                    progress = null,
                    status = null
                )
                onComplete?.invoke(success)
            }
        }
        downloadJobs[modelId] = job
        job.start()
    }

    fun cancelModelDownload(modelId: String) {
        service.cancelModelDownload(modelId)
        val job = downloadJobs.remove(modelId)
        if (job != null) {
            job.cancel()
        } else {
            setModelDownloadState(
                modelId = modelId,
                isBusy = false,
                progress = null,
                status = null
            )
            setMessage("Model download canceled.")
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            setModelDownloadState(
                modelId = modelId,
                isBusy = true,
                progress = null,
                status = "Deleting model..."
            )
            val result = service.deleteModel(modelId)
            refreshModels(
                message = when (result) {
                    is TranslationDownloadResult.Success -> "${result.model.name} was removed."
                    TranslationDownloadResult.Canceled -> "Model action canceled."
                    is TranslationDownloadResult.Failure -> result.message
                }
            )
            setModelDownloadState(
                modelId = modelId,
                isBusy = false,
                progress = null,
                status = null
            )
        }
    }

    fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        onResult: (TranslationResult) -> Unit
    ) {
        val input = text.trim()
        if (input.isEmpty()) {
            onResult(TranslationResult.Failure("Enter a word to translate."))
            return
        }

        val model = service.modelFor(sourceLanguage, targetLanguage)
        if (model == null) {
            onResult(TranslationResult.UnsupportedLanguagePair(sourceLanguage, targetLanguage))
            return
        }

        viewModelScope.launch {
            if (!service.isModelDownloaded(model.id)) {
                refreshModels()
                onResult(TranslationResult.ModelNotDownloaded(model))
                return@launch
            }

            _uiState.update { it.copy(isTranslating = true, message = null) }
            val result = service.translate(input, model.id)
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    message = (result as? TranslationResult.Failure)?.message
                )
            }
            onResult(result)
        }
    }

    private suspend fun refreshModels(message: String? = _uiState.value.message) {
        _uiState.update { it.copy(isRefreshing = true, message = message) }
        val models = service.availableModels().map { model ->
            TranslationModelState(
                model = model,
                isDownloaded = service.isModelDownloaded(model.id),
                isBusy = _uiState.value.models.firstOrNull { it.model.id == model.id }?.isBusy == true,
                downloadProgress = _uiState.value.models.firstOrNull { it.model.id == model.id }?.downloadProgress,
                status = _uiState.value.models.firstOrNull { it.model.id == model.id }?.status
            )
        }
        _uiState.update {
            it.copy(
                isSupported = service.isSupported,
                models = models,
                isRefreshing = false,
                message = message
            )
        }
    }

    private fun setModelDownloadState(
        modelId: String,
        isBusy: Boolean,
        progress: Float?,
        status: String?
    ) {
        _uiState.update { state ->
            state.copy(
                models = state.models.map { modelState ->
                    if (modelState.model.id == modelId) {
                        modelState.copy(
                            isBusy = isBusy,
                            downloadProgress = progress,
                            status = status
                        )
                    } else {
                        modelState
                    }
                }
            )
        }
    }

    private fun setMessage(message: String?) {
        _uiState.update { it.copy(message = message) }
    }
}
