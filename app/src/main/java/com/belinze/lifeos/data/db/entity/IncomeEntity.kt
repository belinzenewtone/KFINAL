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

    @ColumnInfo(name = "amount")                val amount: Double = 0.0,
    @ColumnInfo(name = "source")                val source: String? = null,
    @ColumnInfo(name = "date")                  val date: String? = null,
    @ColumnInfo(name = "note")                  val note: String? = null,
    @ColumnInfo(name = "is_recurring", defaultValue = "0")
                                                val isRecurring: Int = 0,
    @ColumnInfo(name = "frequency")             val frequency: String? = null,
    @ColumnInfo(name = "created_at")            val createdAt: String? = null,
    @ColumnInfo(name = "updated_at")            val updatedAt: String? = null,
    @ColumnInfo(name = "sync_state")            val syncState: String? = null,
    @ColumnInfo(name = "record_source")         val recordSource: String? = null,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String? = null,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int = 1,
    @ColumnInfo(name = "user_id")               val userId: String? = null,
)
