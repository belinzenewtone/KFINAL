package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")               val userId: String? = null,
    @ColumnInfo(name = "title")                 val title: String = "",
    @ColumnInfo(name = "description")           val description: String? = null,
    @ColumnInfo(name = "target_value")          val targetValue: Double = 0.0,
    @ColumnInfo(name = "current_value", defaultValue = "0")
                                                val currentValue: Double = 0.0,
    @ColumnInfo(name = "unit")                  val unit: String? = null,
    @ColumnInfo(name = "category")              val category: String? = null,
    @ColumnInfo(name = "deadline")              val deadline: String? = null,
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String = "active",
    @ColumnInfo(name = "created_at")            val createdAt: String? = null,
    @ColumnInfo(name = "updated_at")            val updatedAt: String? = null,
    @ColumnInfo(name = "sync_state")            val syncState: String? = null,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String? = null,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int = 1,
)
