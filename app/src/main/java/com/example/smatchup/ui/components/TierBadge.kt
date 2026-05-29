package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Tier
import com.example.smatchup.ui.theme.SmatchupColors

private fun Tier.label(): String = when (this) {
    Tier.S_PLUS -> "S+"
    Tier.S -> "S"; Tier.A -> "A"; Tier.B -> "B"; Tier.C -> "C"
    Tier.D -> "D"; Tier.E -> "E"; Tier.F -> "F"
}

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    val color = SmatchupColors.Tier[tier.name] ?: Color.Gray
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tier.label(),
            style = MaterialTheme.typography.titleMedium,
            color = SmatchupColors.Bg1,
        )
    }
}
