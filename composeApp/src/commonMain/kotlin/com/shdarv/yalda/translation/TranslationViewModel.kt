package com.shdarv.yalda.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationUiState(
    val isSupported: Boolean = false,
    val activePair: TranslationPair? = null,
    val languagePacks: List<TranslationLanguagePackState> = emptyList(),
    val isPairReady: Boolean = false,
    val isRefreshing: Boolean = false,
    val isTranslating: Boolean = false,
    val isDownloadingPair: Boolean = false,
    val message: String? = null
)

class TranslationViewModel(
    private val service: LocalTranslationService = localTranslations.get()
) : ViewModel() {
    private var downloadJob: Job? = null
    private val _uiState = MutableStateFlow(
        TranslationUiState(
            isSupported = service.isSupported,
            languagePacks = service.availableLanguages().map(::TranslationLanguagePackState)
        )
    )
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun setActivePair(sourceLanguage: String?, targetLanguage: String?) {
        val pair = if (sourceLanguage == null || targetLanguage == null) null
        else TranslationCatalog.pairFor(sourceLanguage, targetLanguage)
        if (_uiState.value.activePair != pair) {
            _uiState.update { it.copy(activePair = pair, message = null, isPairReady = false) }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshState() }
    }

    fun downloadActivePair(wifiOnly: Boolean = true, onComplete: ((Boolean) -> Unit)? = null) {
        val pair = _uiState.value.activePair ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            var success = false
            try {
                _uiState.update {
                    it.copy(isDownloadingPair = true, message = "Preparing ${pair.displayName}...")
                }
                val result = service.downloadPair(pair, wifiOnly) { progress ->
                    _uiState.update { it.copy(message = progress.message) }
                }
                success = result is TranslationDownloadResult.Success
                refreshState(
                    when (result) {
                        is TranslationDownloadResult.Success -> result.message
                        TranslationDownloadResult.Canceled -> "Translation download canceled."
                        is TranslationDownloadResult.Failure -> result.message
                    }
                )
            } catch (exception: CancellationException) {
                service.cancelPairDownload(pair)
                _uiState.update { it.copy(message = "Translation download canceled.") }
            } finally {
                _uiState.update { it.copy(isDownloadingPair = false) }
                downloadJob = null
                onComplete?.invoke(success)
            }
        }
    }

    fun cancelPairDownload() {
        _uiState.value.activePair?.let(service::cancelPairDownload)
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(isDownloadingPair = false, message = "Translation download canceled.") }
    }

    fun deleteLanguagePack(languageCode: String) {
        viewModelScope.launch {
            setPackBusy(languageCode, true)
            val result = service.deleteLanguagePack(languageCode)
            refreshState(
                when (result) {
                    is TranslationDownloadResult.Success -> result.message
                    TranslationDownloadResult.Canceled -> "Language pack action canceled."
                    is TranslationDownloadResult.Failure -> result.message
                }
            )
            setPackBusy(languageCode, false)
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
        val pair = TranslationCatalog.pairFor(sourceLanguage, targetLanguage)
        if (pair == null) {
            onResult(TranslationResult.UnsupportedLanguagePair(sourceLanguage, targetLanguage))
            return
        }
        if (!service.isSupported) {
            onResult(TranslationResult.Failure("Offline translation is not available on this device."))
            return
        }
        viewModelScope.launch {
            if (!service.isPairReady(pair)) {
                refreshState()
                onResult(TranslationResult.PairNotReady(pair))
                return@launch
            }
            _uiState.update { it.copy(isTranslating = true, message = null) }
            val result = service.translate(input, pair)
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    message = (result as? TranslationResult.Failure)?.message
                )
            }
            onResult(result)
        }
    }

    private suspend fun refreshState(message: String? = _uiState.value.message) {
        _uiState.update { it.copy(isRefreshing = true, message = message) }
        val downloaded = service.downloadedLanguagePacks()
        val currentBusy = _uiState.value.languagePacks.filter { it.isBusy }.map { it.language.code }.toSet()
        val pair = _uiState.value.activePair
        _uiState.update {
            it.copy(
                isSupported = service.isSupported,
                languagePacks = service.availableLanguages().map { language ->
                    TranslationLanguagePackState(
                        language = language,
                        isDownloaded = language.code in downloaded,
                        isBusy = language.code in currentBusy
                    )
                },
                isPairReady = pair != null && downloaded.containsAll(pair.requiredLanguagePacks),
                isRefreshing = false,
                message = message
            )
        }
    }

    private fun setPackBusy(languageCode: String, busy: Boolean) {
        _uiState.update { state ->
            state.copy(languagePacks = state.languagePacks.map { pack ->
                if (pack.language.code == languageCode) pack.copy(isBusy = busy) else pack
            })
        }
    }
}
