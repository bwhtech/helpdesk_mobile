package com.example.helpdeskanalytics.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.helpdeskanalytics.data.local.database.dao.AgentDao
import com.example.helpdeskanalytics.data.local.database.dao.CommentDao
import com.example.helpdeskanalytics.data.local.database.dao.ReportTemplateDao
import com.example.helpdeskanalytics.data.local.database.dao.TeamDao
import com.example.helpdeskanalytics.data.local.database.dao.TicketDao
import com.example.helpdeskanalytics.data.local.database.dao.UserDao
import com.example.helpdeskanalytics.data.local.database.entities.AgentEntity
import com.example.helpdeskanalytics.data.local.database.entities.CommentEntity
import com.example.helpdeskanalytics.data.local.database.entities.ReportTemplateEntity
import com.example.helpdeskanalytics.data.local.database.entities.TeamEntity
import com.example.helpdeskanalytics.data.local.database.entities.TicketEntity
import com.example.helpdeskanalytics.data.local.database.entities.UserEntity

@Database(
    entities = [
        TicketEntity::class, UserEntity::class, CommentEntity::class,
        AgentEntity::class, TeamEntity::class, ReportTemplateEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun userDao(): UserDao
    abstract fun commentDao(): CommentDao
    abstract fun agentDao(): AgentDao
    abstract fun teamDao(): TeamDao
    abstract fun reportTemplateDao(): ReportTemplateDao
}
