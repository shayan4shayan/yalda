package com.shdarv.yalda.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus

private class IosSpeechEngine : SpeechEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recognizer: SFSpeechRecognizer? = SFSpeechRecognizer(locale = NSLocale.currentLocale)
    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var silenceJob: Job? = null
    private var noSpeechJob: Job? = null
    private var activeListeningChanged: ((Boolean) -> Unit)? = null
    private var ignoreRecognitionError: Boolean = false
    private var sessionId: Long = 0L
    private var authorizationStatus: SFSpeechRecognizerAuthorizationStatus =
        SFSpeechRecognizer.authorizationStatus()

    override val isSupported: Boolean = recognizer != null

    @OptIn(ExperimentalForeignApi::class)
    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit
    ) {
        stop()
        val activeSessionId = ++sessionId

        fun isCurrentSession(): Boolean = activeSessionId == sessionId

        fun finishListening() {
            if (isCurrentSession()) {
                onListeningChanged(false)
                activeListeningChanged = null
            }
        }

        fun stopAfterCompletion(finishTask: Boolean = true) {
            if (!isCurrentSession()) return
            val listeningChanged = activeListeningChanged
            sessionId++
            stopRecognition(ignoreError = true, cancelTask = false, finishTask = finishTask)
            listeningChanged?.invoke(false)
            activeListeningChanged = null
        }

        fun completeWithResult(transcription: String) {
            if (!isCurrentSession()) return
            val finalText = transcription.trim()
            if (finalText.isBlank()) {
                stopAfterCompletion(finishTask = true)
                scope.launch { onError("Could not recognize speech.") }
                return
            }
            stopAfterCompletion(finishTask = true)
            scope.launch { onResult(finalText) }
        }

        fun scheduleSilenceTimeout(transcription: String) {
            silenceJob?.cancel()
            silenceJob = scope.launch {
                delay(SILENCE_TIMEOUT_MS)
                if (isCurrentSession()) {
                    completeWithResult(transcription)
                }
            }
        }

        if (!isSupported) {
            finishListening()
            onError("Speech recognition is not available on this device.")
            return
        }

        if (authorizationStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined) {
            SFSpeechRecognizer.requestAuthorization { status ->
                authorizationStatus = status
            }
            finishListening()
            onError("Speech permission required.")
            return
        }
        if (authorizationStatus != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
            finishListening()
            onError("Speech permission denied.")
            return
        }

        ignoreRecognitionError = false

        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
        audioSession.setActive(true, error = null)

        val engine = AVAudioEngine()
        audioEngine = engine
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        recognitionRequest = request

        val inputNode = engine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 1024u,
            format = recordingFormat
        ) { buffer, _ ->
            buffer?.let {
                request.appendAudioPCMBuffer(it)
            }
        }

        engine.prepare()
        if (!engine.startAndReturnError(null)) {
            stop()
            finishListening()
            onError("Could not start speech recognition.")
            return
        }
        activeListeningChanged = onListeningChanged
        onListeningChanged(true)
        noSpeechJob = scope.launch {
            delay(NO_SPEECH_TIMEOUT_MS)
            if (isCurrentSession()) {
                stopAfterCompletion(finishTask = true)
                onError("No speech detected.")
            }
        }

        recognitionTask = recognizer?.recognitionTaskWithRequest(request) { result, error ->
            if (!isCurrentSession()) return@recognitionTaskWithRequest
            if (error != null) {
                if (ignoreRecognitionError) return@recognitionTaskWithRequest
                scope.launch {
                    if (!isCurrentSession()) return@launch
                    stopAfterCompletion(finishTask = false)
                    onError("Could not recognize speech.")
                }
                return@recognitionTaskWithRequest
            }
            val transcription = result?.bestTranscription?.formattedString
            if (!transcription.isNullOrBlank()) {
                noSpeechJob?.cancel()
                scheduleSilenceTimeout(transcription)
            }
            if (result?.isFinal() == true) {
                scope.launch {
                    if (!isCurrentSession()) return@launch
                    if (transcription.isNullOrBlank()) {
                        stopAfterCompletion(finishTask = false)
                        onError("Could not recognize speech.")
                    } else {
                        completeWithResult(transcription)
                    }
                }
            }
        }
    }

    override fun stop() {
        sessionId++
        stopRecognition(ignoreError = true)
        activeListeningChanged?.invoke(false)
        activeListeningChanged = null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun stopRecognition(
        cancelTimers: Boolean = true,
        ignoreError: Boolean = false,
        cancelTask: Boolean = true,
        finishTask: Boolean = false
    ) {
        if (ignoreError) {
            ignoreRecognitionError = true
        }
        if (cancelTimers) {
            silenceJob?.cancel()
            noSpeechJob?.cancel()
        }
        silenceJob = null
        noSpeechJob = null
        val task = recognitionTask
        recognitionTask = null
        val request = recognitionRequest
        recognitionRequest = null
        val engine = audioEngine
        audioEngine = null
        engine?.stop()
        engine?.inputNode?.removeTapOnBus(0u)
        request?.endAudio()
        when {
            finishTask -> task?.finish()
            cancelTask -> task?.cancel()
        }
        AVAudioSession.sharedInstance().setActive(false, error = null)
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private companion object {
        const val SILENCE_TIMEOUT_MS = 1_500L
        const val NO_SPEECH_TIMEOUT_MS = 7_000L
    }
}

private class IosTtsEngine : TtsEngine {
    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String) {
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(NSLocale.currentLocale.localeIdentifier)
        synthesizer.speakUtterance(utterance)
    }
}

@Composable
actual fun rememberSpeechEngine(): SpeechEngine {
    val engine = remember { IosSpeechEngine() }
    DisposableEffect(engine) {
        onDispose { engine.release() }
    }
    return engine
}

@Composable
actual fun rememberTtsEngine(): TtsEngine = remember { IosTtsEngine() }
