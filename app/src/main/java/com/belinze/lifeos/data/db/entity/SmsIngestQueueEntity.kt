package com.belinze.lifeos.data.db.entity

data class SmsIngestQueueEntity(
    val id: Long = 0,
    val body: String,
    val bodyHash: String,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val receivedAt: String?,
    val nextRetryAt: String?,
    val claimedAt: String?,
    val senderAddress: String,
)
