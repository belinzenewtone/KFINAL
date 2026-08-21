package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_ingest_queue",
    indices = [
        Index("status", "next_retry_at"),
        Index("status", "claimed_at"),
        Index(value = ["body_hash"], unique = true),
    ]
)
data class SmsIngestQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "body_hash") val bodyHash: String,
    @ColumnInfo(name = "status",    defaultValue = "pending")
                                                    val status: String,
    @ColumnInfo(name = "attempts",  defaultValue = "0")
                                                    val attempts: Int,
    @ColumnInfo(name = "last_error") val lastError: String?,
    @ColumnInfo(name = "received_at") val receivedAt: String?,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: String?,
    @ColumnInfo(name = "claimed_at") val claimedAt: String?,
    @ColumnInfo(name = "sender_address", defaultValue = "")
                                                    val senderAddress: String,
)
