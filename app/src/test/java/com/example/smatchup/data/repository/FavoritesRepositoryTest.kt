package com.example.smatchup.data.repository

import com.example.smatchup.data.local.dao.FavoritesDao
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeFavoritesDao : FavoritesDao {
    val chars = MutableStateFlow<List<FavoriteCharacterEntity>>(emptyList())
    val mus = MutableStateFlow<List<FavoriteMatchupEntity>>(emptyList())

    override suspend fun addCharacter(fav: FavoriteCharacterEntity) {
        chars.value = chars.value.filterNot { it.userId == fav.userId && it.charId == fav.charId } + fav
    }
    override suspend fun removeCharacter(userId: Long, charId: String) {
        chars.value = chars.value.filterNot { it.userId == userId && it.charId == charId }
    }
    override fun observeCharacters(userId: Long): Flow<List<FavoriteCharacterEntity>> =
        chars.map { list -> list.filter { it.userId == userId } }
    override suspend fun isCharacterFavorite(userId: Long, charId: String): Boolean =
        chars.value.any { it.userId == userId && it.charId == charId }

    override suspend fun addMatchup(fav: FavoriteMatchupEntity) {
        mus.value = mus.value.filterNot { it.userId == fav.userId && it.charA == fav.charA && it.charB == fav.charB } + fav
    }
    override suspend fun removeMatchup(userId: Long, charA: String, charB: String) {
        mus.value = mus.value.filterNot { it.userId == userId && it.charA == charA && it.charB == charB }
    }
    override fun observeMatchups(userId: Long): Flow<List<FavoriteMatchupEntity>> =
        mus.map { list -> list.filter { it.userId == userId } }
    override suspend fun isMatchupFavorite(userId: Long, charA: String, charB: String): Boolean =
        mus.value.any { it.userId == userId && it.charA == charA && it.charB == charB }
}

class FavoritesRepositoryTest {

    private fun repo() = FavoritesRepository(FakeFavoritesDao())

    @Test fun toggleCharacterAddsThenRemoves() = runBlocking {
        val r = repo()
        assertTrue(r.toggleCharacter(1L, "fox"))
        assertTrue(r.isCharacterFavorite(1L, "fox"))
        assertFalse(r.toggleCharacter(1L, "fox"))
        assertFalse(r.isCharacterFavorite(1L, "fox"))
    }

    @Test fun matchupOrderIsNormalized() = runBlocking {
        val r = repo()
        r.toggleMatchup(1L, "sonic", "fox")
        assertTrue(r.isMatchupFavorite(1L, "fox", "sonic"))
        assertTrue(r.isMatchupFavorite(1L, "sonic", "fox"))
    }

    @Test fun observeCharactersReturnsIds() = runBlocking {
        val r = repo()
        r.toggleCharacter(1L, "fox")
        r.toggleCharacter(1L, "steve")
        val ids = r.observeCharacterIds(1L).first()
        assertEquals(setOf("fox", "steve"), ids.toSet())
    }
}
