package io.github.kaulith.helpdeskanalytics.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
