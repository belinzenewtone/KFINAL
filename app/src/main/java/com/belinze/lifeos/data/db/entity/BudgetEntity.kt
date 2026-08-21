package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index("category"),
        Index("deleted_at"),
    ]
)
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "limit_amount") val limitAmount: Double,
    @ColumnInfo(name = "period",    defaultValue = "monthly")
                                                val period: String,
    @ColumnInfo(name = "alert_threshold") val alertThreshold: Double?,
    @ColumnInfo(name = "is_active", defaultValue = "1")
                                                val isActive: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "sync_state") val syncState: String?,
    @ColumnInfo(name = "record_source") val recordSource: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id") val userId: String?,
)
