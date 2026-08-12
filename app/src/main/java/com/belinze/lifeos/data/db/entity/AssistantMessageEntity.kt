package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assistant_messages",
    indices = [
        Index("conversation_id"),
        Index("created_at"),
    ]
)
data class AssistantMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "conversation_id")   val conversationId: String,
    @ColumnInfo(name = "role")              val role: String,       // "user" | "assistant"
    @ColumnInfo(name = "content")           val content: String,
    /** JSON array of suggested follow-up action strings */
    @ColumnInfo(name = "actions")           val actions: String?,
    @ColumnInfo(name = "created_at")        val createdAt: String?,
    @ColumnInfo(name = "updated_at")        val updatedAt: String?,
    @ColumnInfo(name = "sync_state")        val syncState: String?,
    @ColumnInfo(name = "record_source")     val recordSource: String?,
    @ColumnInfo(name = "deleted_at")        val deletedAt: String?,
    @ColumnInfo(name = "revision", defaultValue = "1")
                                            val revision: Int,
    @ColumnInfo(name = "user_id")           val userId: String?,
)
