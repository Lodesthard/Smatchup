// app/src/main/java/com/example/smatchup/SmatchupApp.kt
package com.example.smatchup

import android.app.Application
import com.example.smatchup.data.work.BestPlayersWorker
import com.example.smatchup.di.AppContainer

class SmatchupApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        BestPlayersWorker.schedule(this)
    }

    companion object {
        lateinit var instance: SmatchupApp
            private set
    }
}
