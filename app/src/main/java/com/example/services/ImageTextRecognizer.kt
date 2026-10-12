package com.example.services

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device OCR used as a fallback when a picture is attached but the active
 * engine cannot consume images directly (e.g. local / Ollama models).
 *
 * Uses the bundled ML Kit Latin text recognizer, so it works fully offline.
 */
object ImageTextRecognizer {

    suspend fun recognizeText(bytes: ByteArray): String {
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull() ?: return ""

        return suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    runCatching { recognizer.close() }
                    if (cont.isActive) cont.resume(visionText.text.orEmpty())
                }
                .addOnFailureListener {
                    runCatching { recognizer.close() }
                    if (cont.isActive) cont.resume("")
                }
        }
    }
}
