package com.belinze.lifeos.data.db.entity

data class LearningSessionEntity(
    val id: String,
    val title: String = "",
    val category: String = "General",
    val description: String? = null,
    val durationMinutes: Int = 0,
    val isCompleted: Int = 0,
    val loggedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
)
