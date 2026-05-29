package com.example.smatchup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.glow

enum class GlowButtonVariant { PRIMARY, GHOST }

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlowButtonVariant = GlowButtonVariant.PRIMARY,
    enabled: Boolean = true,
) {
    val fillBrush = when (variant) {
        GlowButtonVariant.PRIMARY -> Brush.horizontalGradient(
            listOf(SmatchupColors.Purple, SmatchupColors.Gold),
        )
        GlowButtonVariant.GHOST -> Brush.horizontalGradient(
            listOf(Color.Transparent, Color.Transparent),
        )
    }
    val textColor = when (variant) {
        GlowButtonVariant.PRIMARY -> SmatchupColors.Bg1
        GlowButtonVariant.GHOST -> SmatchupColors.Gold
    }

    val shape = RoundedCornerShape(50)
    val baseModifier = modifier
        .glow(SmatchupColors.Gold, radius = 8.dp, alpha = if (enabled) 0.4f else 0.0f)
        .clip(shape)
        .background(fillBrush)

    val withBorder = if (variant == GlowButtonVariant.GHOST) {
        baseModifier.border(BorderStroke(1.dp, SmatchupColors.Gold), shape)
    } else baseModifier

    Box(
        modifier = withBorder
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
