package com.belinze.lifeos.data.db.entity

data class AssistantMessageEntity(
    val id: String,
    val conversationId: String = "default",
    val role: String = "",       // "user" | "assistant"
    val content: String = "",
    /** JSON array of suggested follow-up action strings */
    val actions: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String? = null,
    val recordSource: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
    val userId: String? = null,
)
