// app/src/main/java/com/example/smatchup/data/local/SmatchupDatabase.kt
package com.example.smatchup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
    // DAOs added in Task 10
}
