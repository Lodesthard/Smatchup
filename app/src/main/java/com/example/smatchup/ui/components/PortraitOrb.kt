package com.example.smatchup.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    pulse: Boolean = true,
    contentDescription: String? = null,
) {
    // §8.6 breathing glow. Disabled (pulse = false) on list-dense screens to avoid overdraw.
    val glowAlpha = if (pulse) {
        val transition = rememberInfiniteTransition(label = "portraitOrb")
        val animated by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )
        animated
    } else {
        0.5f
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(semanticsModifier)
            .size(size)
            .glow(color = secondary, radius = size / 4, alpha = glowAlpha)
            .clip(CircleShape)
            .orbBackground(primary, secondary),
    )
}
