package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun WinBar(percentA: Float, sample: Int, modifier: Modifier = Modifier) {
    val a = percentA.coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(a.coerceAtLeast(0.001f))
                .fillMaxWidth()
                .background(SmatchupColors.Gold),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "${(a * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = SmatchupColors.Bg1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight((1f - a).coerceAtLeast(0.001f))
                .fillMaxWidth()
                .background(SmatchupColors.DangerRed),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                "${((1 - a) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = SmatchupColors.Bg1,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
