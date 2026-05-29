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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smatchup.R
import com.example.smatchup.SmatchupApp
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onResolved: (loggedIn: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sessionCheck: suspend () -> Boolean = { SmatchupApp.instance.container.authRepository.currentUserId() != null },
) {
    LaunchedEffect(Unit) {
        delay(600)
        onResolved(sessionCheck())
    }
    Column(
        modifier = modifier.wolBackground(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingOrb(size = 96.dp)
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
        )
    }
}
