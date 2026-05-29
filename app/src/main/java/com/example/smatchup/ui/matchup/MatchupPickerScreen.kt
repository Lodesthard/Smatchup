package com.example.smatchup.ui.matchup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.OrbCard
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun MatchupPickerScreen(
    onPairReady: (a: String, b: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchupPickerViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.ready) {
        state.ready?.let { (a, b) ->
            onPairReady(a, b)
            viewModel.reset()
        }
    }

    Column(modifier = modifier.wolBackground().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.matchup_title),
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
            modifier = Modifier.padding(top = 24.dp),
        )
        val prompt = when {
            state.slotA == null -> stringResource(R.string.matchup_pick_first)
            else -> stringResource(R.string.matchup_pick_second, state.slotA!!)
        }
        Text(prompt, color = SmatchupColors.TextDim, style = MaterialTheme.typography.bodyLarge)

        if (state.isLoading) {
            LoadingOrb()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = state.roster, key = { it.id }) { c ->
                    OrbCard(label = c.name, charId = c.id, onClick = { viewModel.pick(c.id) })
                }
            }
        }
    }
}
