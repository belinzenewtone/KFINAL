package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counterparty_overrides",
    indices = [Index(value = ["phone_hash"], unique = true)]
)
data class CounterpartyOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** SHA-256 of normalised phone number */
    @ColumnInfo(name = "phone_hash")    val phoneHash: String,
    @ColumnInfo(name = "display_name")  val displayName: String,
    @ColumnInfo(name = "created_at")    val createdAt: String?,
    @ColumnInfo(name = "updated_at")    val updatedAt: String?,
    @ColumnInfo(name = "user_id")       val userId: String?,
)
