package com.shdarv.yalda.platform

import androidx.compose.runtime.Composable

interface SpeechEngine {
    val isSupported: Boolean
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit = {}
    )
    fun stop()
}

interface TtsEngine {
    fun speak(text: String)
}

@Composable
expect fun rememberSpeechEngine(): SpeechEngine

@Composable
expect fun rememberTtsEngine(): TtsEngine
