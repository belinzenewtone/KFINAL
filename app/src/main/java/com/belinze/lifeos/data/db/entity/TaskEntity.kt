package com.belinze.lifeos.data.db.entity

data class TaskEntity(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val priority: String = "medium",
    val deadline: String? = null,
    val status: String = "active",
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    /** JSON array of reminder offset strings */
    val reminderOffsets: String? = null,
    val alarmEnabled: Int = 0,
    val syncState: String? = null,
    val recordSource: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
    val userId: String? = null,
    val timeSpentSeconds: Int = 0,
)
