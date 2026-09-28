package com.belinze.lifeos.data.db.entity

data class BillEntity(
    val id: String,
    val userId: String? = null,
    val title: String = "",
    val amount: Double? = null,
    val cycle: String? = null,
    val nextDueDate: String? = null,
    val lastPaidAt: String? = null,
    val notes: String? = null,
    val isActive: Int = 1,
    val paidStatus: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
)
