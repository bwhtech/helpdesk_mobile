package com.example.helpdeskanalytics.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val name: String,
    val ticketId: String,
    val content: String,
    val commentedBy: String,
    val createdAt: Instant,
    val commentType: String
)
