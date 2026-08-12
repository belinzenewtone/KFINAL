package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "file_path")     val filePath: String?,
    @ColumnInfo(name = "file_size")     val fileSize: Long?,
    @ColumnInfo(name = "format")        val format: String?,
    @ColumnInfo(name = "created_at")    val createdAt: String?,
    @ColumnInfo(name = "record_count")  val recordCount: Int?,
)
