package com.example.smatchup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.glow
import com.example.smatchup.ui.theme.orbBackground

@Composable
fun PortraitOrb(
    charId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    primary: Color = SmatchupColors.Gold,
    secondary: Color = SmatchupColors.Purple,
) {
    Box(
        modifier = modifier
            .size(size)
            .glow(color = secondary, radius = size / 4, alpha = 0.5f)
            .clip(CircleShape)
            .orbBackground(primary, secondary),
    )
}
