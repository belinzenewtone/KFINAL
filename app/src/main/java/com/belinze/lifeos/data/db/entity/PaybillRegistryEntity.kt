package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paybill_registry")
data class PaybillRegistryEntity(
    @PrimaryKey
    @ColumnInfo(name = "paybill_number")
    val paybillNumber: String,

    @ColumnInfo(name = "display_name")          val displayName: String?,
    @ColumnInfo(name = "last_seen_at")          val lastSeenAt: String?,
    @ColumnInfo(name = "usage_count", defaultValue = "0")
                                                val usageCount: Int,
    @ColumnInfo(name = "last_amount_kes")       val lastAmountKes: Double?,
    @ColumnInfo(name = "user_id")               val userId: String?,
)
