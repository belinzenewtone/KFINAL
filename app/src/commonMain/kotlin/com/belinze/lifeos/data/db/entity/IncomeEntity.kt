package com.belinze.lifeos.data.db.entity

data class IncomeEntity(
    val id: String,
    val amount: Double = 0.0,
    val source: String? = null,
    val date: String? = null,
    val note: String? = null,
    val isRecurring: Int = 0,
    val frequency: String? = null,
    val isActive: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String? = null,
    val recordSource: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
    val userId: String? = null,
)
