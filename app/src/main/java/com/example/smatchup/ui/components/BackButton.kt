package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.smatchup.R
import com.example.smatchup.ui.theme.SmatchupColors

/** Circular back arrow, used as a top-start overlay on drill-down screens. */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .clip(CircleShape)
            .background(SmatchupColors.Bg1.copy(alpha = 0.55f)),
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = SmatchupColors.Gold,
        )
    }
}
