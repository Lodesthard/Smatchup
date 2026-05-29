package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smatchup.R
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun TokenGatedBanner(
    feature: String,
    instructions: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SmatchupColors.Purple.copy(alpha = 0.18f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.token_gated_title, feature),
            style = MaterialTheme.typography.titleMedium,
            color = SmatchupColors.Gold,
        )
        Text(
            text = instructions,
            style = MaterialTheme.typography.bodyMedium,
            color = SmatchupColors.TextDim,
        )
    }
}
