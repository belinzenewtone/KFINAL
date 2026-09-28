package com.belinze.lifeos.data.db.entity

data class BudgetEntity(
    val id: String,
    val category: String,
    val limitAmount: Double,
    val period: String,
    val alertThreshold: Double?,
    val isActive: Int,
    val createdAt: String?,
    val updatedAt: String?,
    val syncState: String?,
    val recordSource: String?,
    val deletedAt: String?,
    val revision: Int,
    val userId: String?,
)
