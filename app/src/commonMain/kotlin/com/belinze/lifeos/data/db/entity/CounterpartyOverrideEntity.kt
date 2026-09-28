package com.belinze.lifeos.data.db.entity

data class CounterpartyOverrideEntity(
    val id: String,
    /** SHA-256 of normalised phone number */
    val phoneHash: String,
    val displayName: String,
    val createdAt: String?,
    val updatedAt: String?,
    val userId: String?,
)
