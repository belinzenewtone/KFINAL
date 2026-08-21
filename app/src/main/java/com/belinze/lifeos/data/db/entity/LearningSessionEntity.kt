package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_sessions",
    indices   = [
        Index("category"),
        Index("logged_at"),
        Index("is_completed"),
    ],
)
data class LearningSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "category") val category: String = "General",
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "duration_minutes", defaultValue = "0")
                                          val durationMinutes: Int = 0,
    @ColumnInfo(name = "is_completed",    defaultValue = "0")
                                          val isCompleted: Int = 0,
    @ColumnInfo(name = "logged_at") val loggedAt: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
)
