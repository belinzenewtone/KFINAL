package com.belinze.lifeos.data.db.entity

data class ImportAuditEntity(
    val id: Long = 0,
    val mpesaCode: String?,
    val rawMessage: String?,
    val amount: Double?,
    val merchant: String?,
    val outcome: String?,
    val failureReason: String?,
    val confidence: String?,
    val createdAt: String?,
)
