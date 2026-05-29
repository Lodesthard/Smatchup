package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun CombosTab(combos: List<String>, modifier: Modifier = Modifier) {
    if (combos.isEmpty()) {
        EmptyState(message = stringResource(R.string.combos_empty), modifier = modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        combos.forEachIndexed { i, combo ->
            Text("${i + 1}. $combo", style = MaterialTheme.typography.bodyLarge, color = SmatchupColors.Text)
        }
    }
}
