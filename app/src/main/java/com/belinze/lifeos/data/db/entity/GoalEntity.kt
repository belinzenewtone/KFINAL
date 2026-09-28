package com.belinze.lifeos.data.db.entity

data class GoalEntity(
    val id: String,
    val userId: String? = null,
    val title: String = "",
    val description: String? = null,
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val unit: String? = null,
    val category: String? = null,
    val deadline: String? = null,
    val status: String = "active",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
)
