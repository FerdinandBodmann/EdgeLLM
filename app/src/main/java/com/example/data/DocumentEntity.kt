package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-imported document that participates in local RAG.
 * Full extracted text is stored on-device only.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mimeType: String = "text/plain",
    val sizeBytes: Long = 0,
    val sourceUri: String = "",
    val importedAt: Long = System.currentTimeMillis(),
    val charCount: Int = 0,
    val tokenEstimate: Int = 0,
    val text: String
)