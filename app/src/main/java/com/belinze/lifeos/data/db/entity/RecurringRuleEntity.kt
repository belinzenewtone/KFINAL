package com.belinze.lifeos.data.db.entity

data class RecurringRuleEntity(
    val id: String,
    val title: String = "",
    val type: String? = null,
    val cadence: String? = null,
    val nextRunAt: String? = null,
    val amount: Double? = null,
    val category: String? = null,
    val enabled: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String? = null,
    val recordSource: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
    val userId: String? = null,
)
