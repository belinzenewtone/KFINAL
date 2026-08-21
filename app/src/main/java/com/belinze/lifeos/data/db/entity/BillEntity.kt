package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    indices = [Index("next_due_date")]
)
data class BillEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "amount") val amount: Double? = null,
    @ColumnInfo(name = "cycle") val cycle: String? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDate: String? = null,
    @ColumnInfo(name = "last_paid_at") val lastPaidAt: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "is_active", defaultValue = "1")
                                                val isActive: Int = 1,
    @ColumnInfo(name = "paid_status", defaultValue = "0")
                                                val paidStatus: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "sync_state") val syncState: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int = 1,
)
