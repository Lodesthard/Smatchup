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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.glow
import com.example.smatchup.ui.theme.orbBackground

@Composable
fun LoadingOrb(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val transition = rememberInfiniteTransition(label = "loadingOrb")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Box(
        modifier = modifier
            .size(size)
            .alpha(pulse)
            .glow(SmatchupColors.Purple, radius = size / 3, alpha = 0.6f)
            .clip(CircleShape)
            .orbBackground(SmatchupColors.Gold, SmatchupColors.Purple),
    )
}
