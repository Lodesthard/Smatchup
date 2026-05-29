package com.example.smatchup.ui.components

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.glow
import com.example.smatchup.ui.theme.orbBackground

private val drawableCache = mutableMapOf<String, Int>()

/** Resolves a `char_<id>` drawable for the given character id, or 0 if none is bundled. */
private fun charDrawable(context: Context, charId: String): Int =
    drawableCache.getOrPut(charId) {
        context.resources.getIdentifier("char_$charId", "drawable", context.packageName)
    }

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
    val context = LocalContext.current
    val portraitRes = remember(charId) { charDrawable(context, charId) }

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

    val base = modifier
        .then(semanticsModifier)
        .size(size)
        .glow(color = secondary, radius = size / 4, alpha = glowAlpha)
        .clip(CircleShape)

    if (portraitRes != 0) {
        Image(
            painter = painterResource(portraitRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base,
        )
    } else {
        // Fallback for ids without a bundled portrait (and decorative Home tiles).
        Box(modifier = base.orbBackground(primary, secondary))
    }
}
