package com.belinze.lifeos.data.db.entity

data class EventEntity(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val date: String = "",
    val endDate: String? = null,
    val type: String = "event",
    val kind: String = "other",
    val importance: String = "medium",
    val status: String = "active",
    val hasReminder: Int = 0,
    val reminderMinutesBefore: Int? = null,
    /** JSON array of reminder offset strings */
    val reminderOffsets: String? = null,
    val reminderTimeOfDayMinutes: Int? = null,
    val allDay: Int = 0,
    val repeatRule: String = "none",
    val repeatEndDate: String? = null,
    val location: String? = null,
    /** JSON array of guest strings */
    val guests: String? = null,
    val timeZoneId: String = "UTC",
    val alarmEnabled: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncState: String? = null,
    val recordSource: String? = null,
    val deletedAt: String? = null,
    val revision: Int = 1,
    val userId: String? = null,
)
