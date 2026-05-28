package com.example.smatchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smatchup.ui.nav.SmatchupNavGraph
import com.example.smatchup.ui.theme.SmatchupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmatchupTheme {
                SmatchupNavGraph()
            }
        }
    }
}
