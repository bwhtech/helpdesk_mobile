package com.example.helpdeskanalytics.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val fullName: String?,
    val roles: List<String>,
    val hasTeamLeadPermission: Boolean
)
