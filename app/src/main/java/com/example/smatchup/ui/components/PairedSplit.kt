package com.example.smatchup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun PairedSplit(
    leftWho: String,
    leftItems: List<String>,
    rightWho: String,
    rightItems: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Side(who = leftWho, items = leftItems, modifier = Modifier.weight(1f))
        Side(who = rightWho, items = rightItems, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Side(who: String, items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = who.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = SmatchupColors.TextDim,
        )
        if (items.isEmpty()) {
            Text("—", color = SmatchupColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        } else {
            items.forEach { Text("• $it", color = SmatchupColors.Text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
