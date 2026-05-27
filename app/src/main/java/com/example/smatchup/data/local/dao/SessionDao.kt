package com.example.smatchup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smatchup.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = 0 LIMIT 1")
    suspend fun get(): SessionEntity?

    @Query("SELECT * FROM session WHERE id = 0 LIMIT 1")
    fun observe(): Flow<SessionEntity?>
}
