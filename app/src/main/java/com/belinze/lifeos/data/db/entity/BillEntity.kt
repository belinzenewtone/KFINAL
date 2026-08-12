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

    @ColumnInfo(name = "user_id")               val userId: String?,
    @ColumnInfo(name = "title")                 val title: String,
    @ColumnInfo(name = "amount")                val amount: Double?,
    @ColumnInfo(name = "cycle")                 val cycle: String?,
    @ColumnInfo(name = "next_due_date")         val nextDueDate: String?,
    @ColumnInfo(name = "last_paid_at")          val lastPaidAt: String?,
    @ColumnInfo(name = "notes")                 val notes: String?,
    @ColumnInfo(name = "is_active", defaultValue = "1")
                                                val isActive: Int,
    @ColumnInfo(name = "paid_status", defaultValue = "0")
                                                val paidStatus: Int,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    @ColumnInfo(name = "sync_state")            val syncState: String?,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
)
