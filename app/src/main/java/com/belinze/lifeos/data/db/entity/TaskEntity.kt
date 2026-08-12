package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("deadline"),
        Index("status"),
        Index("deleted_at"),
        Index("status", "deadline"),
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")                 val title: String,
    @ColumnInfo(name = "description")           val description: String?,
    @ColumnInfo(name = "priority",  defaultValue = "medium")
                                                val priority: String,
    @ColumnInfo(name = "deadline")              val deadline: String?,
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String,
    @ColumnInfo(name = "completed_at")          val completedAt: String?,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    /** JSON array of reminder offset strings */
    @ColumnInfo(name = "reminder_offsets")      val reminderOffsets: String?,
    @ColumnInfo(name = "alarm_enabled", defaultValue = "0")
                                                val alarmEnabled: Int,
    @ColumnInfo(name = "sync_state")            val syncState: String?,
    @ColumnInfo(name = "record_source")         val recordSource: String?,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id")               val userId: String?,
    @ColumnInfo(name = "time_spent_seconds", defaultValue = "0")
                                                val timeSpentSeconds: Int,
)
