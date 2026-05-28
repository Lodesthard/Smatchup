package com.example.smatchup.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glow(color: Color, radius: Dp = 16.dp, alpha: Float = 0.6f): Modifier =
    this.drawBehind {
        val rad = radius.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = (size.maxDimension / 2f) + rad,
            ),
        )
    }

fun Modifier.orbBackground(primary: Color, secondary: Color): Modifier =
    this.background(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primary, secondary),
            center = Offset.Unspecified,
            radius = Float.POSITIVE_INFINITY,
        ),
    )

fun Modifier.wolBackground(): Modifier = this
    .fillMaxSize()
    .background(
        brush = Brush.verticalGradient(
            0.0f to SmatchupColors.Bg1,
            1.0f to SmatchupColors.Bg2,
        ),
    )
