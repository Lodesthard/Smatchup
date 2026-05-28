package com.example.smatchup.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.OrbCard
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

data class HomeRoutes(
    val onCharacters: () -> Unit,
    val onMatchup: () -> Unit,
    val onTierlist: () -> Unit,
    val onFavorites: () -> Unit,
    val onProfile: () -> Unit,
    val onLastMatch: () -> Unit,
)

@Composable
fun HomeScreen(
    routes: HomeRoutes,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .wolBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
            Text(
                text = "Smatchup",
                style = MaterialTheme.typography.displayLarge,
                color = SmatchupColors.Gold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OrbCard(label = "Characters",  onClick = routes.onCharacters, modifier = Modifier.weight(1f))
            OrbCard(label = "Match-up",    onClick = routes.onMatchup,    modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OrbCard(label = "Tier Lists",  onClick = routes.onTierlist,   modifier = Modifier.weight(1f))
            OrbCard(label = "Favorites",   onClick = routes.onFavorites,  modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OrbCard(label = "Profile",     onClick = routes.onProfile,    modifier = Modifier.weight(1f))
            OrbCard(label = "Last Match",  onClick = routes.onLastMatch,  modifier = Modifier.weight(1f))
        }
    }
}
