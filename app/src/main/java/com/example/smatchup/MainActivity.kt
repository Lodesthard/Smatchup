package com.example.smatchup

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smatchup.i18n.LocaleManager
import com.example.smatchup.ui.nav.SmatchupNavGraph
import com.example.smatchup.ui.theme.SmatchupTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmatchupTheme {
                SmatchupNavGraph()
            }
        }
    }
}
