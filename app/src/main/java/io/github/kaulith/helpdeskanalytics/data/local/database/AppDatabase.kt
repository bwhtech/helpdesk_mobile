package io.github.kaulith.helpdeskanalytics.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.AgentDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.CommentDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.ReportTemplateDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.TeamDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.TicketDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.UserDao
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.AgentEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.CommentEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.ReportTemplateEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.TeamEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.TicketEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.UserEntity

@Database(
    entities = [
        TicketEntity::class, UserEntity::class, CommentEntity::class,
        AgentEntity::class, TeamEntity::class, ReportTemplateEntity::class
    ],
    version = 6,
    exportSchema = true
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
