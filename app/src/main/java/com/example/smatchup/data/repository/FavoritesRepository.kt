package com.example.smatchup.data.repository

import com.example.smatchup.data.local.dao.FavoritesDao
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(private val dao: FavoritesDao) {

    /** Returns the new favorite state (true = now a favorite). */
    suspend fun toggleCharacter(userId: Long, charId: String): Boolean {
        return if (dao.isCharacterFavorite(userId, charId)) {
            dao.removeCharacter(userId, charId)
            false
        } else {
            dao.addCharacter(FavoriteCharacterEntity(userId, charId, System.currentTimeMillis()))
            true
        }
    }

    suspend fun isCharacterFavorite(userId: Long, charId: String): Boolean =
        dao.isCharacterFavorite(userId, charId)

    fun observeCharacterIds(userId: Long): Flow<List<String>> =
        dao.observeCharacters(userId).map { list -> list.map { it.charId } }

    /** Returns the new favorite state (true = now a favorite). */
    suspend fun toggleMatchup(userId: Long, a: String, b: String): Boolean {
        val (x, y) = normalize(a, b)
        return if (dao.isMatchupFavorite(userId, x, y)) {
            dao.removeMatchup(userId, x, y)
            false
        } else {
            dao.addMatchup(FavoriteMatchupEntity(userId, x, y, System.currentTimeMillis()))
            true
        }
    }

    suspend fun isMatchupFavorite(userId: Long, a: String, b: String): Boolean {
        val (x, y) = normalize(a, b)
        return dao.isMatchupFavorite(userId, x, y)
    }

    fun observeMatchupPairs(userId: Long): Flow<List<Pair<String, String>>> =
        dao.observeMatchups(userId).map { list -> list.map { it.charA to it.charB } }

    private fun normalize(a: String, b: String): Pair<String, String> =
        if (a <= b) a to b else b to a
}
