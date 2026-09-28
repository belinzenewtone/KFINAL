package com.belinze.lifeos.data.db.entity

data class UserProfileEntity(
    val id: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val avatarUri: String?,
    val createdAt: String?,
    val updatedAt: String?,
)
