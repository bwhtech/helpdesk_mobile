package io.github.kaulith.helpdeskanalytics.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.ReportTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportTemplateDao {

    @Query("SELECT * FROM report_templates ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ReportTemplateEntity>>

    @Query("SELECT * FROM report_templates WHERE id = :id")
    suspend fun getById(id: Long): ReportTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReportTemplateEntity): Long

    @Update
    suspend fun update(entity: ReportTemplateEntity)

    @Query("DELETE FROM report_templates WHERE id = :id")
    suspend fun delete(id: Long)
}
