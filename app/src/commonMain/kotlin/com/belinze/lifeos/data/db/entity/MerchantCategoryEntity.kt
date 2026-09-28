package com.belinze.lifeos.data.db.entity

data class MerchantCategoryEntity(
    val id: String,
    val merchant: String,
    val category: String?,
    val confidence: Double,
    val userCorrected: Int,
    val createdAt: String?,
    val updatedAt: String?,
    val syncState: String?,
    val recordSource: String?,
    val deletedAt: String?,
    val revision: Int,
    val userId: String?,
)
