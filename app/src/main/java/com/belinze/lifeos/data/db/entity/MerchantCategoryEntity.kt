package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_categories",
    indices = [Index(value = ["merchant"], unique = true)]
)
data class MerchantCategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "merchant") val merchant: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "confidence", defaultValue = "0")
                                                val confidence: Double,
    @ColumnInfo(name = "user_corrected", defaultValue = "0")
                                                val userCorrected: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "sync_state") val syncState: String?,
    @ColumnInfo(name = "record_source") val recordSource: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    @ColumnInfo(name = "revision",  defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id") val userId: String?,
)
