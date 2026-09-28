package com.belinze.lifeos.data.db.entity

data class ExportEntity(
    val id: String,
    val filePath: String?,
    val fileSize: Long?,
    val format: String?,
    val createdAt: String?,
    val recordCount: Int?,
)
