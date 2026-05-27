package com.example.smatchup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smatchup.data.local.dao.CacheDao
import com.example.smatchup.data.local.dao.FavoritesDao
import com.example.smatchup.data.local.dao.SessionDao
import com.example.smatchup.data.local.dao.UserDao
import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
import com.example.smatchup.data.local.entity.CachedFramedataEntity
import com.example.smatchup.data.local.entity.CachedWinrateEntity
import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import com.example.smatchup.data.local.entity.SessionEntity
import com.example.smatchup.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        FavoriteCharacterEntity::class,
        FavoriteMatchupEntity::class,
        CachedFramedataEntity::class,
        CachedWinrateEntity::class,
        CachedBestPlayerEntity::class,
        CachedYoutubeVideoEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SmatchupDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun cacheDao(): CacheDao
    abstract fun sessionDao(): SessionDao
}
