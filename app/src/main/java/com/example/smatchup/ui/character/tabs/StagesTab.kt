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
import com.example.smatchup.domain.model.Stage
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun StagesTab(ban: List<Stage>, counterpick: List<Stage>, modifier: Modifier = Modifier) {
    if (ban.isEmpty() && counterpick.isEmpty()) {
        EmptyState(message = stringResource(R.string.stages_empty), modifier = modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (ban.isNotEmpty()) {
            Text(stringResource(R.string.stages_ban), style = MaterialTheme.typography.titleMedium, color = SmatchupColors.DangerRed)
            ban.forEach { Text("• ${it.displayName}", color = SmatchupColors.Text) }
        }
        if (counterpick.isNotEmpty()) {
            Text(stringResource(R.string.stages_counterpick), style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Gold)
            counterpick.forEach { Text("• ${it.displayName}", color = SmatchupColors.Text) }
        }
    }
}
