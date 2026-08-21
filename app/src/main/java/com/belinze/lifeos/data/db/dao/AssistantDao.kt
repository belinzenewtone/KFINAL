package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.AssistantMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    /** Rolling 10-message history window for the conversation context. */
    @Query("""
        SELECT * FROM assistant_messages
        WHERE deleted_at IS NULL AND conversation_id = :conversationId
        ORDER BY created_at DESC
        LIMIT 10
    """)
    suspend fun getHistory(conversationId: String): List<AssistantMessageEntity>

    @Query("""
        SELECT * FROM assistant_messages
        WHERE deleted_at IS NULL AND conversation_id = :conversationId
        ORDER BY created_at ASC
    """)
    fun observeConversation(conversationId: String): Flow<List<AssistantMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: AssistantMessageEntity)

    @Query("UPDATE assistant_messages SET deleted_at = :ts WHERE conversation_id = :conversationId")
    suspend fun clearConversation(conversationId: String, ts: String)
}
