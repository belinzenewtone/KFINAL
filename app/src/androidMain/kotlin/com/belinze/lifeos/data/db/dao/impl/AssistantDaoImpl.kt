package com.belinze.lifeos.data.db.dao.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.belinze.lifeos.data.db.AssistantMessageQueries
import com.belinze.lifeos.data.db.Assistant_messages
import com.belinze.lifeos.data.db.dao.AssistantDao
import com.belinze.lifeos.data.db.entity.AssistantMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssistantDaoImpl(private val q: AssistantMessageQueries) : AssistantDao {
    override suspend fun getHistory(conversationId: String): List<AssistantMessageEntity> =
        q.getHistory(conversationId).executeAsList().map { it.toEntity() }

    override fun observeConversation(conversationId: String): Flow<List<AssistantMessageEntity>> =
        q.observeConversation(conversationId).asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }

    override suspend fun insert(msg: AssistantMessageEntity) = q.insertOrReplace(
        id           = msg.id,
        conversationId = msg.conversationId,
        role         = msg.role,
        content      = msg.content,
        actions      = msg.actions,
        createdAt    = msg.createdAt,
        updatedAt    = msg.updatedAt,
        syncState    = msg.syncState,
        recordSource = msg.recordSource,
        deletedAt    = msg.deletedAt,
        revision     = msg.revision.toLong(),
        userId       = msg.userId,
    )

    override suspend fun clearConversation(conversationId: String, ts: String) =
        q.clearConversation(ts, conversationId)
}

private fun Assistant_messages.toEntity() = AssistantMessageEntity(
    id             = id,
    conversationId = conversation_id,
    role           = role,
    content        = content,
    actions        = actions,
    createdAt      = created_at,
    updatedAt      = updated_at,
    syncState      = sync_state,
    recordSource   = record_source,
    deletedAt      = deleted_at,
    revision       = revision.toInt(),
    userId         = user_id,
)
