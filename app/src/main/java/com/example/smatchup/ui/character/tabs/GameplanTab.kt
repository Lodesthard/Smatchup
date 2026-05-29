package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smatchup.R
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun GameplanTab(gameplan: String?, modifier: Modifier = Modifier) {
    if (gameplan.isNullOrBlank()) {
        EmptyState(message = stringResource(R.string.gameplan_empty), modifier = modifier)
        return
    }
    Text(
        text = gameplan,
        style = MaterialTheme.typography.bodyLarge,
        color = SmatchupColors.Text,
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    )
}
