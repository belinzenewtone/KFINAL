package com.belinze.lifeos.data.db.entity

data class FulizaLoanEntity(
    val id: String,
    val drawCode: String? = null,
    val drawAmountKes: Double = 0.0,
    val totalRepaidKes: Double = 0.0,
    val status: String = "active",
    val drawDate: String? = null,
    val lastRepaymentDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val userId: String? = null,
)
