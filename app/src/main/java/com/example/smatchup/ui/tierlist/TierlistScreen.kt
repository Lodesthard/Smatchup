package com.example.smatchup.ui.tierlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
import com.example.smatchup.domain.model.TierGroup
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.components.TierBadge
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun TierlistScreen(
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TierlistViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.wolBackground().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.tierlist_title),
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
            modifier = Modifier.padding(top = 24.dp),
        )
        Toggle(selected = state.selectedName, onSelect = viewModel::select)

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            state.view == null || state.view!!.groups.isEmpty() ->
                EmptyState(message = stringResource(R.string.tierlist_unavailable))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                items(items = state.view!!.groups, key = { it.tier.name }) { group ->
                    TierRow(group = group, onCharacterClick = onCharacterClick)
                }
            }
        }
    }
}

@Composable
private fun Toggle(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SmatchupColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
    ) {
        ToggleOpt("strength", stringResource(R.string.tier_strength), selected, onSelect, Modifier.weight(1f))
        ToggleOpt("difficulty", stringResource(R.string.tier_difficulty), selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun ToggleOpt(name: String, label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier) {
    val active = name == selected
    Box(
        modifier = modifier
            .background(if (active) SmatchupColors.Purple.copy(alpha = 0.4f) else SmatchupColors.Bg2)
            .clickable { onSelect(name) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) SmatchupColors.Gold else SmatchupColors.TextDim,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TierRow(group: TierGroup, onCharacterClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SmatchupColors.Bg2.copy(alpha = 0.5f))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TierBadge(tier = group.tier)
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            group.characters.forEach { c ->
                // 48 dp clickable target (≥ a11y minimum) around the 40 dp orb.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onCharacterClick(c.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    PortraitOrb(
                        charId = c.id,
                        size = 40.dp,
                        pulse = false,
                        contentDescription = c.name,
                    )
                }
            }
        }
    }
}
