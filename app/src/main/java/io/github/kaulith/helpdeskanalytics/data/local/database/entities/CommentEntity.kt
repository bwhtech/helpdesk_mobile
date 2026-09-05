package io.github.kaulith.helpdeskanalytics.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val name: String,
    val ticketId: String,
    val content: String,
    val commentedBy: String,
    val createdAt: Instant,
    val commentType: String
)
