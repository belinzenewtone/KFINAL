package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incomes",
    indices = [Index("date")]
)
data class IncomeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "amount")                val amount: Double,
    @ColumnInfo(name = "source")                val source: String?,
    @ColumnInfo(name = "date")                  val date: String?,
    @ColumnInfo(name = "note")                  val note: String?,
    @ColumnInfo(name = "is_recurring", defaultValue = "0")
                                                val isRecurring: Int,
    @ColumnInfo(name = "frequency")             val frequency: String?,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    @ColumnInfo(name = "sync_state")            val syncState: String?,
    @ColumnInfo(name = "record_source")         val recordSource: String?,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id")               val userId: String?,
)
