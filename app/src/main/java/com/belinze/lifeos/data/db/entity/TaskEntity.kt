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
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "priority",  defaultValue = "medium")
                                                val priority: String = "medium",
    @ColumnInfo(name = "deadline") val deadline: String? = null,
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String = "active",
    @ColumnInfo(name = "completed_at") val completedAt: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    /** JSON array of reminder offset strings */
    @ColumnInfo(name = "reminder_offsets") val reminderOffsets: String? = null,
    @ColumnInfo(name = "alarm_enabled", defaultValue = "0")
                                                val alarmEnabled: Int = 0,
    @ColumnInfo(name = "sync_state") val syncState: String? = null,
    @ColumnInfo(name = "record_source") val recordSource: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int = 1,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "time_spent_seconds", defaultValue = "0")
                                                val timeSpentSeconds: Int = 0,
)
