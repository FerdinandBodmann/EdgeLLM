package com.example.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Thin, app-wide wrapper around Android [TextToSpeech] used to read assistant
 * messages aloud. Initialization is asynchronous, so requests that arrive
 * before the engine is ready are queued and flushed once it is.
 */
object SpeechOutput {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    /** Invoked (off the main thread) when an utterance completes or fails. */
    var onFinished: (() -> Unit)? = null

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                runCatching { tts?.language = Locale.getDefault() }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        onFinished?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onFinished?.invoke()
                    }
                })
                pendingText?.let { speak(it) }
                pendingText = null
            }
        }
    }

    fun isReady(): Boolean = ready

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val engine = tts
        if (engine == null || !ready) {
            pendingText = clean
            return
        }
        runCatching {
            engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "synapse_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        pendingText = null
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        pendingText = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
