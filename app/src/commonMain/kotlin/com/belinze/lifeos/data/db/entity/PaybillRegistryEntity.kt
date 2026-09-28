package com.belinze.lifeos.data.db.entity

data class PaybillRegistryEntity(
    val paybillNumber: String,
    val displayName: String?,
    val lastSeenAt: String?,
    val usageCount: Int,
    val lastAmountKes: Double?,
    val userId: String?,
)
