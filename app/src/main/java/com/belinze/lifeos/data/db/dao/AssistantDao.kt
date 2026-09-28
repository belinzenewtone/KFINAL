package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.AssistantMessageEntity
import kotlinx.coroutines.flow.Flow
interface AssistantDao {
    /** Rolling 10-message history window for the conversation context. */
        suspend fun getHistory(conversationId: String): List<AssistantMessageEntity>

    /**
     * RFINAL caps the loaded conversation at 100 messages. The inner query takes the
     * newest 100 (DESC) and the outer re-sorts them oldest-first for display, so a long
     * conversation loses its oldest messages rather than its most recent ones.
     */
        fun observeConversation(conversationId: String): Flow<List<AssistantMessageEntity>>

        suspend fun insert(msg: AssistantMessageEntity)

        suspend fun clearConversation(conversationId: String, ts: String)
}
