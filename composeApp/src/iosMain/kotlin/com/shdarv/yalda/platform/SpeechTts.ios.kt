package com.shdarv.yalda.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.NSLog
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
        speechLog("startListening requested currentSession=$sessionId")
        stopRecognition(ignoreError = true)
        activeListeningChanged?.invoke(false)
        activeListeningChanged = null
        val activeSessionId = ++sessionId
        speechLog(
            "startListening session=$activeSessionId supported=$isSupported speechAuth=$authorizationStatus"
        )

        fun isCurrentSession(): Boolean = activeSessionId == sessionId

        fun finishListening() {
            if (isCurrentSession()) {
                onListeningChanged(false)
                activeListeningChanged = null
            }
        }

        fun stopAfterCompletion(finishTask: Boolean = true) {
            if (!isCurrentSession()) return
            speechLog("stopAfterCompletion session=$activeSessionId finishTask=$finishTask")
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
                speechLog("completeWithResult session=$activeSessionId blank transcription")
                stopAfterCompletion(finishTask = true)
                scope.launch { onError("Could not recognize speech.") }
                return
            }
            speechLog("completeWithResult session=$activeSessionId textLength=${finalText.length}")
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
            speechLog("startListening session=$activeSessionId unsupported recognizer")
            finishListening()
            onError("Speech recognition is not available on this device.")
            return
        }

        if (authorizationStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined) {
            speechLog("requesting speech permission session=$activeSessionId")
            SFSpeechRecognizer.requestAuthorization { status ->
                authorizationStatus = status
                speechLog("speech permission callback session=$activeSessionId status=$status")
            }
            finishListening()
            onError("Speech permission required.")
            return
        }
        if (authorizationStatus != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
            speechLog("speech permission denied session=$activeSessionId status=$authorizationStatus")
            finishListening()
            onError("Speech permission denied.")
            return
        }

        ignoreRecognitionError = false

        val audioSession = AVAudioSession.sharedInstance()
        speechLog(
            "microphone permission session=$activeSessionId recordPermission=${audioSession.recordPermission}"
        )
        when (audioSession.recordPermission) {
            AVAudioSessionRecordPermissionUndetermined -> {
                speechLog("requesting microphone permission session=$activeSessionId")
                audioSession.requestRecordPermission { granted ->
                    speechLog(
                        "microphone permission callback session=$activeSessionId granted=$granted"
                    )
                    scope.launch {
                        if (isCurrentSession()) {
                            finishListening()
                            onError(
                                if (granted) {
                                    "Microphone permission granted. Tap speak again."
                                } else {
                                    "Microphone permission denied."
                                }
                            )
                        }
                    }
                }
                finishListening()
                onError("Microphone permission required.")
                return
            }
            AVAudioSessionRecordPermissionDenied -> {
                speechLog("microphone permission denied session=$activeSessionId")
                finishListening()
                onError("Microphone permission denied.")
                return
            }
            AVAudioSessionRecordPermissionGranted -> Unit
        }

        val categorySet = audioSession.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            AVAudioSessionModeMeasurement,
            AVAudioSessionCategoryOptionDefaultToSpeaker,
            null
        )
        val modeSet = audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
        val activeSet = audioSession.setActive(true, error = null)
        speechLog(
            "audio session configured session=$activeSessionId categorySet=$categorySet modeSet=$modeSet activeSet=$activeSet"
        )

        val engine = AVAudioEngine()
        audioEngine = engine
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        recognitionRequest = request

        val inputNode = engine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        speechLog(
            "input format session=$activeSessionId ${recordingFormat.speechFormatDescription()}"
        )
        if (recordingFormat.channelCount == 0u || recordingFormat.sampleRate <= 0.0) {
            speechLog("invalid input format session=$activeSessionId")
            stopRecognition(ignoreError = true)
            finishListening()
            onError("No microphone input is available.")
            return
        }

        speechLog("creating recognition task session=$activeSessionId")
        recognitionTask = recognizer?.recognitionTaskWithRequest(request) { result, error ->
            if (!isCurrentSession()) return@recognitionTaskWithRequest
            if (error != null) {
                speechLog(
                    "recognition error session=$activeSessionId ignore=$ignoreRecognitionError error=$error"
                )
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
                speechLog(
                    "recognition partial session=$activeSessionId final=${result.isFinal()} textLength=${transcription.length}"
                )
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
        if (recognitionTask == null) {
            speechLog("recognition task null session=$activeSessionId")
            stopRecognition(ignoreError = true)
            finishListening()
            onError("Speech recognition is not available on this device.")
            return
        }

        var tapLogCount = 0
        var skippedTapLogCount = 0
        speechLog("installing tap session=$activeSessionId bufferSize=4096")
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 4096u,
            format = recordingFormat
        ) { buffer, _ ->
            buffer?.let {
                val byteSize = it.firstAudioBufferByteSize()
                val isEmptyBuffer = it.frameLength == 0u || byteSize == 0u
                val shouldLogBuffer =
                    tapLogCount < MAX_TAP_LOGS ||
                        (isEmptyBuffer && skippedTapLogCount < MAX_SKIPPED_TAP_LOGS)
                if (shouldLogBuffer) {
                    tapLogCount++
                    speechLog(
                        "tap buffer session=$activeSessionId current=${isCurrentSession()} frameLength=${it.frameLength} frameCapacity=${it.frameCapacity} byteSize=$byteSize stride=${it.stride}"
                    )
                }
                if (isCurrentSession() && it.frameLength > 0u && byteSize != 0u) {
                    request.appendAudioPCMBuffer(it)
                } else if (isCurrentSession()) {
                    if (skippedTapLogCount < MAX_SKIPPED_TAP_LOGS) {
                        skippedTapLogCount++
                        speechLog(
                            "skipping tap buffer session=$activeSessionId frameLength=${it.frameLength} byteSize=$byteSize"
                        )
                    } else if (skippedTapLogCount == MAX_SKIPPED_TAP_LOGS) {
                        skippedTapLogCount++
                        speechLog("further skipped tap buffer logs suppressed session=$activeSessionId")
                    }
                }
            }
        }

        engine.prepare()
        speechLog("starting audio engine session=$activeSessionId")
        if (!engine.startAndReturnError(null)) {
            speechLog("audio engine failed to start session=$activeSessionId")
            stop()
            finishListening()
            onError("Could not start speech recognition.")
            return
        }
        speechLog("audio engine started session=$activeSessionId")
        activeListeningChanged = onListeningChanged
        onListeningChanged(true)
        noSpeechJob = scope.launch {
            delay(NO_SPEECH_TIMEOUT_MS)
            if (isCurrentSession()) {
                speechLog("no speech timeout session=$activeSessionId")
                stopAfterCompletion(finishTask = true)
                onError("No speech detected.")
            }
        }
    }

    override fun stop() {
        speechLog("stop requested currentSession=$sessionId")
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
        speechLog(
            "stopRecognition session=$sessionId cancelTimers=$cancelTimers ignoreError=$ignoreError cancelTask=$cancelTask finishTask=$finishTask hasEngine=${audioEngine != null} hasRequest=${recognitionRequest != null} hasTask=${recognitionTask != null}"
        )
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
        engine?.inputNode?.removeTapOnBus(0u)
        engine?.stop()
        request?.endAudio()
        when {
            finishTask -> task?.finish()
            cancelTask -> task?.cancel()
        }
        val activeCleared = AVAudioSession.sharedInstance().setActive(false, error = null)
        speechLog("audio session deactivated session=$sessionId activeCleared=$activeCleared")
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private companion object {
        const val SILENCE_TIMEOUT_MS = 1_500L
        const val NO_SPEECH_TIMEOUT_MS = 7_000L
        const val MAX_TAP_LOGS = 8
        const val MAX_SKIPPED_TAP_LOGS = 20
    }
}

private fun speechLog(message: String) {
    NSLog("YaldaSpeech: $message")
}

@OptIn(ExperimentalForeignApi::class)
private fun AVAudioFormat.speechFormatDescription(): String {
    val stream = streamDescription?.pointed
    return "sampleRate=$sampleRate channelCount=$channelCount bytesPerFrame=${stream?.mBytesPerFrame} framesPerPacket=${stream?.mFramesPerPacket} formatID=${stream?.mFormatID}"
}

@OptIn(ExperimentalForeignApi::class)
private fun AVAudioPCMBuffer.firstAudioBufferByteSize(): UInt? {
    val bufferList = audioBufferList ?: return null
    val pointedList = bufferList.pointed
    if (pointedList.mNumberBuffers == 0u) return 0u
    return pointedList.mBuffers.pointed.mDataByteSize
}

private class IosTtsEngine : TtsEngine {
    private val synthesizer = AVSpeechSynthesizer()

    @OptIn(ExperimentalForeignApi::class)
    override fun speak(text: String) {
        val audioSession = AVAudioSession.sharedInstance()
        val categorySet = audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
        val modeSet = audioSession.setMode(AVAudioSessionModeDefault, error = null)
        val activeSet = audioSession.setActive(true, error = null)
        speechLog(
            "tts audio session configured categorySet=$categorySet modeSet=$modeSet activeSet=$activeSet"
        )

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
