package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun CombosTab(combos: List<String>, modifier: Modifier = Modifier) {
    if (combos.isEmpty()) {
        EmptyState(message = "Pas encore de combos pour ce personnage.", modifier = modifier)
        return
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        combos.forEachIndexed { i, combo ->
            Text("${i + 1}. $combo", style = MaterialTheme.typography.bodyLarge, color = SmatchupColors.Text)
        }
    }
}
