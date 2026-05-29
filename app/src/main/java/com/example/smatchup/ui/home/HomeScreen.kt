package com.example.smatchup.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smatchup.R
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
    val tiles = listOf(
        stringResource(R.string.home_characters) to routes.onCharacters,
        stringResource(R.string.home_matchup) to routes.onMatchup,
        stringResource(R.string.home_tierlists) to routes.onTierlist,
        stringResource(R.string.home_favorites) to routes.onFavorites,
        stringResource(R.string.home_profile) to routes.onProfile,
        stringResource(R.string.home_last_match) to routes.onLastMatch,
    )

    Column(
        modifier = modifier
            .wolBackground()
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = SmatchupColors.Gold,
            )
        }
        // Adaptive grid: 2 columns on a phone in portrait, more on landscape / tablets.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items = tiles) { (label, onClick) ->
                OrbCard(label = label, onClick = onClick)
            }
        }
    }
}
