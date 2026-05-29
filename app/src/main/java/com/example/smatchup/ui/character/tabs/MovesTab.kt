package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun MovesTab(movesUtility: Map<String, String>, modifier: Modifier = Modifier) {
    if (movesUtility.isEmpty()) {
        EmptyState(message = stringResource(R.string.moves_empty), modifier = modifier)
        return
    }
    val entries = movesUtility.entries.toList()
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(items = entries, key = { it.key }) { (moveId, util) ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    text = moveId.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = SmatchupColors.Gold,
                )
                Text(
                    text = util,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmatchupColors.Text,
                )
            }
        }
    }
}
