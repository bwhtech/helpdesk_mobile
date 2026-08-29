package io.github.kaulith.helpdeskanalytics.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE ticketId = :ticketId ORDER BY createdAt ASC")
    fun getCommentsForTicket(ticketId: String): Flow<List<CommentEntity>>

    @Upsert
    suspend fun upsertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE ticketId = :ticketId")
    suspend fun deleteCommentsForTicket(ticketId: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAllComments()
}
