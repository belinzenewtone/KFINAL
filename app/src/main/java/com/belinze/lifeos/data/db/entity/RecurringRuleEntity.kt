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

    @ColumnInfo(name = "title")                 val title: String,
    @ColumnInfo(name = "type")                  val type: String?,
    @ColumnInfo(name = "cadence")               val cadence: String?,
    @ColumnInfo(name = "next_run_at")           val nextRunAt: String?,
    @ColumnInfo(name = "amount")                val amount: Double?,
    @ColumnInfo(name = "category")              val category: String?,
    @ColumnInfo(name = "enabled",   defaultValue = "1")
                                                val enabled: Int,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    @ColumnInfo(name = "sync_state")            val syncState: String?,
    @ColumnInfo(name = "record_source")         val recordSource: String?,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id")               val userId: String?,
)
