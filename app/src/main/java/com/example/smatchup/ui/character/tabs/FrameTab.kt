package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun FrameTab(moves: List<Move>, source: FramedataSource, modifier: Modifier = Modifier) {
    if (moves.isEmpty()) {
        EmptyState(message = stringResource(R.string.frame_empty), modifier = modifier)
        return
    }
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.frame_source, source.name.lowercase()),
            style = MaterialTheme.typography.labelMedium,
            color = SmatchupColors.TextDim,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.frame_col_move), color = SmatchupColors.Gold, modifier = Modifier.weight(2f))
            Text(stringResource(R.string.frame_col_startup), color = SmatchupColors.Gold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.frame_col_total), color = SmatchupColors.Gold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.frame_col_dmg), color = SmatchupColors.Gold, modifier = Modifier.weight(1f))
        }
        LazyColumn {
            items(items = moves, key = { it.id }) { m ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(m.displayName, color = SmatchupColors.Text, modifier = Modifier.weight(2f))
                    Text(m.frame.startup?.toString() ?: "—", color = SmatchupColors.Text, modifier = Modifier.weight(1f))
                    Text(m.frame.totalFrames?.toString() ?: "—", color = SmatchupColors.Text, modifier = Modifier.weight(1f))
                    Text(m.frame.baseDamage?.let { "%.1f".format(it) } ?: "—", color = SmatchupColors.Text, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
