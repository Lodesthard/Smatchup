package com.example.smatchup.ui.character

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun DetailHero(character: Character, modifier: Modifier = Modifier) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val heroModifier = modifier
        .fillMaxWidth()
        .height(if (landscape) 120.dp else 200.dp)
        .background(
            Brush.verticalGradient(
                0.0f to SmatchupColors.Bg2,
                1.0f to SmatchupColors.Bg1,
            ),
        )
        .border(width = 1.dp, color = SmatchupColors.Gold)

    if (landscape) {
        // Wide layout: orb beside the name to fit the shorter landscape hero.
        Row(
            modifier = heroModifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            PortraitOrb(charId = character.id, size = 64.dp, contentDescription = character.name)
            Column {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = SmatchupColors.Gold,
                )
                Text(
                    text = "${character.series} · #${character.rosterNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SmatchupColors.TextDim,
                )
            }
        }
    } else {
        Box(
            modifier = heroModifier.padding(top = 32.dp, bottom = 12.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PortraitOrb(charId = character.id, size = 90.dp, contentDescription = character.name)
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = SmatchupColors.Gold,
                )
                Text(
                    text = "${character.series} · #${character.rosterNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SmatchupColors.TextDim,
                )
            }
        }
    }
}
