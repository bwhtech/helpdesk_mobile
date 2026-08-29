package io.github.kaulith.helpdeskanalytics.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: String): TicketEntity?

    @Upsert
    suspend fun upsertTickets(tickets: List<TicketEntity>)

    @Transaction
    suspend fun replaceAll(tickets: List<TicketEntity>) {
        deleteAllTickets()
        upsertTickets(tickets)
    }

    @Query("DELETE FROM tickets")
    suspend fun deleteAllTickets()

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun getTicketCount(): Int
}
