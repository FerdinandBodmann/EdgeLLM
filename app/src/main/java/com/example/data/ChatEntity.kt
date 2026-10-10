package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val activeModelId: String = "nanollm-embedded"
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: Long,
    val role: String, // "user" or "assistant" or "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensCount: Int = 0,
    val tokensPerSec: Float = 0f,
    val timeToFirstTokenMs: Long = 0L,
    val modelName: String = ""
)
