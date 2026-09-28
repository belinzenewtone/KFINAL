package com.belinze.lifeos.data.db.entity

/**
 * Room entity for the `transactions` table.
 * Schema is authoritative — verified against src/database/schema.ts.
 */
data class TransactionEntity(
    val id: String,
    val amount: Double = 0.0,
    val merchant: String? = null,
    val category: String? = null,
    val date: String? = null,
    val source: String? = null,
    val transactionType: String? = null,
    val mpesaCode: String? = null,
    val sourceHash: String? = null,
    val rawSms: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val balanceAfter: Double? = null,
    val fee: Double? = null,
    val status: String = "completed",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String = "pending",
    val recordSource: String = "manual",
    val deletedAt: String? = null,
    val revision: Int = 1,
    val userId: String? = null,
    val inferredCategory: Int = 0,
    val inferenceSource: String? = null,
    val semanticHash: String? = null,
    val institutionId: String = "mpesa",
    val externalRef: String? = null,
    val currency: String = "KES",
    val rawSender: String = "",
)
