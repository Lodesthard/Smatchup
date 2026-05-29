package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smatchup.R
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.TokenGatedBanner
import com.example.smatchup.ui.components.YouTubePlayer
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun VideoTab(
    videoUrl: String?,
    videoTitle: String?,
    tokenGated: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            tokenGated -> TokenGatedBanner(
                feature = stringResource(R.string.video_feature),
                instructions = stringResource(R.string.video_instructions),
            )
            videoUrl != null -> {
                if (videoTitle != null) {
                    Text(videoTitle, style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Gold)
                }
                YouTubePlayer(videoIdOrUrl = videoUrl)
            }
            else -> EmptyState(message = stringResource(R.string.video_empty))
        }
    }
}
