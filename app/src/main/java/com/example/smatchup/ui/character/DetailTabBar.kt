package com.example.smatchup.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun DetailTabBar(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SmatchupColors.Bg1.copy(alpha = 0.92f))
            .border(width = 1.dp, color = SmatchupColors.Purple.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState()),
    ) {
        DetailTab.entries.forEach { tab ->
            val isActive = tab == selected
            Text(
                text = tab.label.uppercase(),
                color = if (isActive) SmatchupColors.Gold else SmatchupColors.TextDim,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}
