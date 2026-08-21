package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_rules",
    indices = [Index("next_run_at")]
)
data class RecurringRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "cadence") val cadence: String? = null,
    @ColumnInfo(name = "next_run_at") val nextRunAt: String? = null,
    @ColumnInfo(name = "amount") val amount: Double? = null,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "enabled",   defaultValue = "1")
                                                val enabled: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "sync_state") val syncState: String? = null,
    @ColumnInfo(name = "record_source") val recordSource: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int = 1,
    @ColumnInfo(name = "user_id") val userId: String? = null,
)
