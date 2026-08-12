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

    @ColumnInfo(name = "title")                 val title: String,
    @ColumnInfo(name = "description")           val description: String?,
    @ColumnInfo(name = "date")                  val date: String,
    @ColumnInfo(name = "end_date")              val endDate: String?,
    @ColumnInfo(name = "type",      defaultValue = "event")
                                                val type: String,
    @ColumnInfo(name = "kind",      defaultValue = "other")
                                                val kind: String,
    @ColumnInfo(name = "importance", defaultValue = "medium")
                                                val importance: String,
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String,
    @ColumnInfo(name = "has_reminder", defaultValue = "0")
                                                val hasReminder: Int,
    @ColumnInfo(name = "reminder_minutes_before")
                                                val reminderMinutesBefore: Int?,
    /** JSON array of reminder offset strings */
    @ColumnInfo(name = "reminder_offsets")      val reminderOffsets: String?,
    @ColumnInfo(name = "reminder_time_of_day_minutes")
                                                val reminderTimeOfDayMinutes: Int?,
    @ColumnInfo(name = "all_day",   defaultValue = "0")
                                                val allDay: Int,
    @ColumnInfo(name = "repeat_rule", defaultValue = "none")
                                                val repeatRule: String,
    @ColumnInfo(name = "repeat_end_date")       val repeatEndDate: String?,
    @ColumnInfo(name = "location")              val location: String?,
    /** JSON array of guest strings */
    @ColumnInfo(name = "guests")                val guests: String?,
    @ColumnInfo(name = "time_zone_id", defaultValue = "UTC")
                                                val timeZoneId: String,
    @ColumnInfo(name = "alarm_enabled", defaultValue = "0")
                                                val alarmEnabled: Int,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    @ColumnInfo(name = "sync_state")            val syncState: String?,
    @ColumnInfo(name = "record_source")         val recordSource: String?,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id")               val userId: String?,
)
