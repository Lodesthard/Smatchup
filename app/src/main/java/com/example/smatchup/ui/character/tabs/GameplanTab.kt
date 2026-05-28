package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun GameplanTab(gameplan: String?, modifier: Modifier = Modifier) {
    if (gameplan.isNullOrBlank()) {
        EmptyState(message = "Pas encore de gameplan pour ce personnage.", modifier = modifier)
        return
    }
    Text(
        text = gameplan,
        style = MaterialTheme.typography.bodyLarge,
        color = SmatchupColors.Text,
        modifier = modifier.padding(16.dp),
    )
}
