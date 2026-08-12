package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")          val name: String?,
    @ColumnInfo(name = "email")         val email: String?,
    @ColumnInfo(name = "phone")         val phone: String?,
    @ColumnInfo(name = "avatar_uri")    val avatarUri: String?,
    @ColumnInfo(name = "created_at")    val createdAt: String?,
    @ColumnInfo(name = "updated_at")    val updatedAt: String?,
)
