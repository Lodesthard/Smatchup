package com.example.smatchup.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.smatchup.R
import com.example.smatchup.ui.theme.SmatchupColors
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
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

    // Many tournament VODs disable embedding (errors 150 / 152). When that happens the inline
    // player can't show them, so we fall back to a tap-to-open-in-YouTube card.
    var embedFailed by remember(id) { mutableStateOf(false) }

    if (embedFailed) {
        OpenInYouTube(id = id, modifier = modifier)
        return
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ctx = LocalContext.current

    val playerView = remember(id) {
        YouTubePlayerView(ctx).apply {
            enableAutomaticInitialization = false
            val options = IFramePlayerOptions.Builder().controls(1).build()
            initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(id, 0f)
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError,
                    ) {
                        if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ||
                            error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND ||
                            error == PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST
                        ) {
                            embedFailed = true
                        }
                    }
                },
                options,
            )
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

@Composable
private fun OpenInYouTube(id: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(SmatchupColors.Bg2)
            .clickable {
                val url = "https://www.youtube.com/watch?v=$id"
                runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = SmatchupColors.Gold,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.video_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = SmatchupColors.TextDim,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.video_open_youtube),
            style = MaterialTheme.typography.titleMedium,
            color = SmatchupColors.Gold,
            textAlign = TextAlign.Center,
        )
    }
}
