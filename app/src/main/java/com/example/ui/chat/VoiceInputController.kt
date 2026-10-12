package com.example.ui.chat

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import com.example.services.SpeechInput

/**
 * Composable-friendly controller around [SpeechRecognizer].
 *
 * Owns a single recognizer instance for the lifetime of the composing UI and
 * surfaces partial / final transcription plus human-readable errors.
 */
class VoiceInputController(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val recognizer: SpeechRecognizer? =
        runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()

    var isListening: Boolean = false
        private set

    var isAvailable: Boolean = SpeechInput.isAvailable(context)
        private set

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                onError(mapError(error))
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onFinal(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onPartial(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun start() {
        val engine = recognizer ?: run {
            onError("Speech recognition is not available on this device")
            return
        }
        runCatching {
            engine.cancel()
            engine.startListening(SpeechInput.createIntent())
            isListening = true
        }.onFailure {
            isListening = false
            onError("Could not start listening")
        }
    }

    fun stop() {
        isListening = false
        runCatching { recognizer?.stopListening() }
    }

    fun destroy() {
        isListening = false
        runCatching { recognizer?.destroy() }
    }

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech recognition needs a network connection"
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
        else -> "Speech recognition error ($error)"
    }
}
