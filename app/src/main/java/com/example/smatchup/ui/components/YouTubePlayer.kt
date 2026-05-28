package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smatchup.ui.theme.SmatchupColors
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

fun parseYouTubeVideoId(input: String): String? {
    if (input.isEmpty()) return null
    if (Regex("^[A-Za-z0-9_-]{11}$").matches(input)) return input
    val patterns = listOf(
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
        Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
        Regex("""embed/([A-Za-z0-9_-]{11})"""),
        Regex("""shorts/([A-Za-z0-9_-]{11})"""),
    )
    for (p in patterns) {
        val m = p.find(input)
        if (m != null) return m.groupValues[1]
    }
    return null
}

@Composable
fun YouTubePlayer(
    videoIdOrUrl: String,
    modifier: Modifier = Modifier,
) {
    val id = remember(videoIdOrUrl) { parseYouTubeVideoId(videoIdOrUrl) }
    if (id == null) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ctx = LocalContext.current

    val playerView = remember(id) {
        YouTubePlayerView(ctx).apply {
            enableAutomaticInitialization = false
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.cueVideo(id, 0f)
                }
            })
        }
    }

    DisposableEffect(playerView) {
        lifecycle.addObserver(playerView)
        onDispose {
            lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(SmatchupColors.Bg1),
        factory = { playerView },
    )
}
