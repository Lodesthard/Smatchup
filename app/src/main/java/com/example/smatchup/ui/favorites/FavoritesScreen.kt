package com.example.smatchup.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun FavoritesScreen(
    onCharacterClick: (String) -> Unit,
    onMatchupClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.wolBackground().fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.favorites_title), style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold, modifier = Modifier.padding(top = 24.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            state.characters.isEmpty() && state.matchups.isEmpty() ->
                EmptyState(message = stringResource(R.string.favorites_empty))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                if (state.characters.isNotEmpty()) {
                    item { Text(stringResource(R.string.favorites_characters), style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Purple) }
                    items(items = state.characters, key = { "c-${it.id}" }) { c -> CharacterFavRow(c, onCharacterClick) }
                }
                if (state.matchups.isNotEmpty()) {
                    item { Text(stringResource(R.string.favorites_matchups), style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Purple) }
                    items(items = state.matchups, key = { "m-${it.first.id}-${it.second.id}" }) { (a, b) ->
                        MatchupFavRow(a, b, onMatchupClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterFavRow(c: Character, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(c.id) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PortraitOrb(charId = c.id, size = 40.dp, pulse = false, contentDescription = c.name)
        Text(c.name, style = MaterialTheme.typography.bodyLarge, color = SmatchupColors.Text)
    }
}

@Composable
private fun MatchupFavRow(a: Character, b: Character, onClick: (String, String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(a.id, b.id) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PortraitOrb(charId = a.id, size = 36.dp, pulse = false, contentDescription = a.name)
        Text(stringResource(R.string.vs), style = MaterialTheme.typography.bodyMedium, color = SmatchupColors.TextDim)
        PortraitOrb(charId = b.id, size = 36.dp, pulse = false, contentDescription = b.name)
        Text("${a.name} / ${b.name}", style = MaterialTheme.typography.bodyMedium, color = SmatchupColors.Text)
    }
}
