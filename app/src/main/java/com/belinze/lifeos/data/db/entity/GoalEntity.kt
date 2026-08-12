package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")               val userId: String?,
    @ColumnInfo(name = "title")                 val title: String,
    @ColumnInfo(name = "description")           val description: String?,
    @ColumnInfo(name = "target_value")          val targetValue: Double,
    @ColumnInfo(name = "current_value", defaultValue = "0")
                                                val currentValue: Double,
    @ColumnInfo(name = "unit")                  val unit: String?,
    @ColumnInfo(name = "category")              val category: String?,
    @ColumnInfo(name = "deadline")              val deadline: String?,
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    @ColumnInfo(name = "sync_state")            val syncState: String?,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
)
