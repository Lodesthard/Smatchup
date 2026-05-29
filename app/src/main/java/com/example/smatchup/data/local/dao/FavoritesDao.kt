package com.example.smatchup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCharacter(fav: FavoriteCharacterEntity)

    @Query("DELETE FROM favorite_characters WHERE userId = :userId AND charId = :charId")
    suspend fun removeCharacter(userId: Long, charId: String)

    @Query("SELECT * FROM favorite_characters WHERE userId = :userId ORDER BY addedAt DESC")
    fun observeCharacters(userId: Long): Flow<List<FavoriteCharacterEntity>>

    @Query("""
        SELECT EXISTS(SELECT 1 FROM favorite_characters
                     WHERE userId = :userId AND charId = :charId)
    """)
    suspend fun isCharacterFavorite(userId: Long, charId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMatchup(fav: FavoriteMatchupEntity)

    @Query("""
        DELETE FROM favorite_matchups
        WHERE userId = :userId AND charA = :charA AND charB = :charB
    """)
    suspend fun removeMatchup(userId: Long, charA: String, charB: String)

    @Query("SELECT * FROM favorite_matchups WHERE userId = :userId ORDER BY addedAt DESC")
    fun observeMatchups(userId: Long): Flow<List<FavoriteMatchupEntity>>

    @Query("""
        SELECT EXISTS(SELECT 1 FROM favorite_matchups
                     WHERE userId = :userId AND charA = :charA AND charB = :charB)
    """)
    suspend fun isMatchupFavorite(userId: Long, charA: String, charB: String): Boolean
}
