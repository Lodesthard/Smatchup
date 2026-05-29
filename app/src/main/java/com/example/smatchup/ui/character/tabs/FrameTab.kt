package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smatchup.R
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

private val MOVE_W = 130.dp
private val NUM_W = 64.dp

@Composable
fun FrameTab(moves: List<Move>, source: FramedataSource, modifier: Modifier = Modifier) {
    if (moves.isEmpty()) {
        EmptyState(message = stringResource(R.string.frame_empty), modifier = modifier)
        return
    }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = stringResource(R.string.frame_source, source.name.lowercase()),
            style = MaterialTheme.typography.labelMedium,
            color = SmatchupColors.TextDim,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        // Horizontally scrollable so all 6 columns stay readable on narrow screens.
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                HeaderCell(stringResource(R.string.frame_col_move), MOVE_W)
                HeaderCell(stringResource(R.string.frame_col_startup), NUM_W)
                HeaderCell(stringResource(R.string.frame_col_total), NUM_W)
                HeaderCell(stringResource(R.string.frame_col_endlag), NUM_W)
                HeaderCell(stringResource(R.string.frame_col_shield), NUM_W)
                HeaderCell(stringResource(R.string.frame_col_dmg), NUM_W)
            }
            moves.forEach { m ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    BodyCell(m.displayName, MOVE_W, SmatchupColors.Text)
                    BodyCell(m.frame.startup?.toString() ?: "—", NUM_W, SmatchupColors.Text)
                    BodyCell(m.frame.totalFrames?.toString() ?: "—", NUM_W, SmatchupColors.Text)
                    BodyCell(m.frame.endLag?.toString() ?: "—", NUM_W, SmatchupColors.Text)
                    BodyCell(shieldText(m.frame.onShield), NUM_W, shieldColor(m.frame.onShield))
                    BodyCell(m.frame.baseDamage?.let { "%.1f".format(it) } ?: "—", NUM_W, SmatchupColors.Text)
                }
            }
        }
    }
}

private fun shieldText(onShield: Int?): String = when {
    onShield == null -> "—"
    onShield > 0 -> "+$onShield"
    else -> onShield.toString()
}

private fun shieldColor(onShield: Int?): Color = when {
    onShield == null -> SmatchupColors.Text
    onShield >= 0 -> SmatchupColors.Gold
    else -> SmatchupColors.DangerRed
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        color = SmatchupColors.Gold,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.width(width).padding(end = 8.dp),
    )
}

@Composable
private fun BodyCell(text: String, width: Dp, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.width(width).padding(end = 8.dp),
    )
}
