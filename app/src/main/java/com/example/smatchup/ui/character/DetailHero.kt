package com.example.smatchup.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun DetailHero(character: Character, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    0.0f to SmatchupColors.Bg2,
                    1.0f to SmatchupColors.Bg1,
                ),
            )
            .border(width = 1.dp, color = SmatchupColors.Gold)
            .padding(top = 32.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PortraitOrb(charId = character.id, size = 90.dp)
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
