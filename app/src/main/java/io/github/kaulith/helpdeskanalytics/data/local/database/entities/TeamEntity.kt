package io.github.kaulith.helpdeskanalytics.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val name: String,
    val members: List<String>,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
