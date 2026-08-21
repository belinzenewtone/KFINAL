package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "import_audit",
    indices = [
        Index("outcome"),
        Index("created_at"),
    ]
)
data class ImportAuditEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "mpesa_code") val mpesaCode: String?,
    @ColumnInfo(name = "raw_message") val rawMessage: String?,
    @ColumnInfo(name = "amount") val amount: Double?,
    @ColumnInfo(name = "merchant") val merchant: String?,
    @ColumnInfo(name = "outcome") val outcome: String?,
    @ColumnInfo(name = "failure_reason") val failureReason: String?,
    @ColumnInfo(name = "confidence") val confidence: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
)
