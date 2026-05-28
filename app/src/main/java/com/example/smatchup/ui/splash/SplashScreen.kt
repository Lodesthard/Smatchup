package com.example.smatchup.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        delay(600)
        onSplashDone()
    }
    Column(
        modifier = modifier.wolBackground(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingOrb(size = 96.dp)
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "Smatchup",
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
        )
    }
}
