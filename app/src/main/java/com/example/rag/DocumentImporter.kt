package com.example.rag

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.DocumentEntity
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports user-selected files into the on-device knowledge base.
 *
 * Supported formats: plain text and code (`txt`, `md`, `markdown`, `csv`,
 * `json`, `log`) and PDF (text layer extracted via PDFBox). All processing is
 * local; nothing is uploaded.
 */
object DocumentImporter {

    private const val MAX_CHARS = 400_000

    suspend fun importFromUri(context: Context, uri: Uri): Result<DocumentEntity> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "text/plain"
                val metadata = queryMetadata(context, uri)
                val displayName = metadata.first
                val sizeBytes = metadata.second
                val extension = displayName.substringAfterLast('.', "").lowercase()

                val rawText = when {
                    extension == "pdf" || mime == "application/pdf" -> extractPdfText(context, uri)
                    else -> extractPlainText(context, uri)
                }.trim()

                if (rawText.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("No readable text found in \"$displayName\".")
                    )
                }

                val capped = if (rawText.length > MAX_CHARS) rawText.substring(0, MAX_CHARS) else rawText

                val document = DocumentEntity(
                    name = displayName,
                    mimeType = mime,
                    sizeBytes = sizeBytes,
                    sourceUri = uri.toString(),
                    charCount = capped.length,
                    tokenEstimate = (capped.length / 4).coerceAtLeast(1),
                    text = capped
                )
                Result.success(document)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private fun queryMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = ""
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: ""
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "document.txt"
        }
        return name to size
    }

    private fun extractPlainText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return input.bufferedReader(Charsets.UTF_8).readText()
        }
        return ""
    }

    private fun extractPdfText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { pdf ->
                return PDFTextStripper().getText(pdf)
            }
        }
        return ""
    }
}
