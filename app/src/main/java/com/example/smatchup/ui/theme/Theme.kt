package com.example.smatchup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SmatchupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary       = SmatchupColors.Purple,
            secondary     = SmatchupColors.Gold,
            background    = SmatchupColors.Bg1,
            surface       = SmatchupColors.Bg2,
            onPrimary     = SmatchupColors.Text,
            onSecondary   = SmatchupColors.Bg1,
            onBackground  = SmatchupColors.Text,
            onSurface     = SmatchupColors.Text,
            error         = SmatchupColors.DangerRed,
        ),
        typography = SmatchupTypography,
        content = content,
    )
}
