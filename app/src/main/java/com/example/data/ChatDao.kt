package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE title LIKE '%' || :query || '%' ORDER BY isPinned DESC, lastMessageAt DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: Long): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Query("UPDATE chats SET title = :newTitle WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: Long, newTitle: String)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :chatId")
    suspend fun setChatPinned(chatId: Long, pinned: Boolean)

    @Query("UPDATE chats SET lastMessageAt = :timestamp WHERE id = :chatId")
    suspend fun touchChat(chatId: Long, timestamp: Long)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: Long)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC, id ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE chatId = :chatId AND timestamp > :timestamp")
    suspend fun deleteMessagesAfter(chatId: Long, timestamp: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND id > :messageId")
    suspend fun deleteMessagesAfterId(chatId: Long, messageId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)
}