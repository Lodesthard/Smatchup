// app/src/main/java/com/example/smatchup/SmatchupApp.kt
package com.example.smatchup

import android.app.Application
import com.example.smatchup.di.AppContainer

class SmatchupApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: SmatchupApp
            private set
    }
}
