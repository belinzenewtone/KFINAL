package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index("date"),
        Index("deleted_at"),
    ]
)
data class EventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")                 val title: String = "",
    @ColumnInfo(name = "description")           val description: String? = null,
    @ColumnInfo(name = "date")                  val date: String = "",
    @ColumnInfo(name = "end_date")              val endDate: String? = null,
    @ColumnInfo(name = "type",      defaultValue = "event")
                                                val type: String = "event",
    @ColumnInfo(name = "kind",      defaultValue = "other")
                                                val kind: String = "other",
    @ColumnInfo(name = "importance", defaultValue = "medium")
                                                val importance: String = "medium",
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String = "active",
    @ColumnInfo(name = "has_reminder", defaultValue = "0")
                                                val hasReminder: Int = 0,
    @ColumnInfo(name = "reminder_minutes_before")
                                                val reminderMinutesBefore: Int? = null,
    /** JSON array of reminder offset strings */
    @ColumnInfo(name = "reminder_offsets")      val reminderOffsets: String? = null,
    @ColumnInfo(name = "reminder_time_of_day_minutes")
                                                val reminderTimeOfDayMinutes: Int? = null,
    @ColumnInfo(name = "all_day",   defaultValue = "0")
                                                val allDay: Int = 0,
    @ColumnInfo(name = "repeat_rule", defaultValue = "none")
                                                val repeatRule: String = "none",
    @ColumnInfo(name = "repeat_end_date")       val repeatEndDate: String? = null,
    @ColumnInfo(name = "location")              val location: String? = null,
    /** JSON array of guest strings */
    @ColumnInfo(name = "guests")                val guests: String? = null,
    @ColumnInfo(name = "time_zone_id", defaultValue = "UTC")
                                                val timeZoneId: String = "UTC",
    @ColumnInfo(name = "alarm_enabled", defaultValue = "0")
                                                val alarmEnabled: Int = 0,
    @ColumnInfo(name = "created_at")            val createdAt: String? = null,
    @ColumnInfo(name = "updated_at")            val updatedAt: String? = null,
    @ColumnInfo(name = "sync_state")            val syncState: String? = null,
    @ColumnInfo(name = "record_source")         val recordSource: String? = null,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String? = null,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int = 1,
    @ColumnInfo(name = "user_id")               val userId: String? = null,
)
