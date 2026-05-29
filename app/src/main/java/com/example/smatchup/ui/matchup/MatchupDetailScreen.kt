package com.example.smatchup.ui.matchup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
import com.example.smatchup.domain.model.Matchup
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.FavoriteHeart
import com.example.smatchup.ui.components.FavoriteToggleViewModel
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.PairedSplit
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.components.SectionCard
import com.example.smatchup.ui.components.TokenGatedBanner
import com.example.smatchup.ui.components.WinBar
import com.example.smatchup.ui.components.YouTubePlayer
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun MatchupDetailScreen(
    viewModel: MatchupDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.wolBackground()) {
        when {
            state.isLoading && state.matchup == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            state.error != null ->
                EmptyState(message = stringResource(R.string.error_generic, state.error ?: ""), ctaText = stringResource(R.string.back), onCta = onBack)
            state.matchup != null -> {
                val mu = state.matchup!!
                Column(modifier = Modifier.fillMaxSize()) {
                    Header(mu = mu, winRateA = state.winRateA, sample = state.winrateSample, tokenGated = state.winrateTokenGated)
                    TabBar(selected = state.selectedTab, onSelect = viewModel::selectTab)
                    Crossfade(
                        targetState = state.selectedTab,
                        animationSpec = tween(180),
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        label = "matchupDetailTab",
                    ) { tab ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (tab) {
                                MatchupDetailTab.GAMEPLAN -> SectionCard(stringResource(R.string.section_gameplan)) {
                                    PairedSplit(mu.charA, mu.gameplanA, mu.charB, mu.gameplanB)
                                }
                                MatchupDetailTab.STRONG -> SectionCard(stringResource(R.string.section_strong_moves)) {
                                    PairedSplit(mu.charA, mu.strongMovesA, mu.charB, mu.strongMovesB)
                                }
                                MatchupDetailTab.PUNISH -> SectionCard(stringResource(R.string.section_punishable)) {
                                    PairedSplit(mu.charA, mu.punishableMovesA, mu.charB, mu.punishableMovesB)
                                }
                                MatchupDetailTab.STAGES -> SectionCard(stringResource(R.string.section_stages)) {
                                    PairedSplit(
                                        mu.charA, mu.stagesForA.map { "${it.verdict.name.lowercase()}: ${it.displayName}" },
                                        mu.charB, mu.stagesForB.map { "${it.verdict.name.lowercase()}: ${it.displayName}" },
                                    )
                                }
                                MatchupDetailTab.VIDEO -> {
                                    if (state.lastVideoUrl != null) {
                                        if (state.lastVideoTitle != null) {
                                            Text(state.lastVideoTitle!!, style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Gold)
                                        }
                                        YouTubePlayer(videoIdOrUrl = state.lastVideoUrl!!)
                                    } else {
                                        EmptyState(message = stringResource(R.string.mu_no_vod))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        state.matchup?.let { mu ->
            FavoriteHeart(
                viewModel = viewModel<FavoriteToggleViewModel>(
                    factory = ViewModelFactory.fromApp().favoriteMatchup(mu.charA, mu.charB),
                    key = "fav-mu-${mu.charA}-${mu.charB}",
                ),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun Header(mu: Matchup, winRateA: Float?, sample: Int, tokenGated: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            CharCol(mu.charA)
            Text(stringResource(R.string.vs_caps), style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold)
            CharCol(mu.charB)
        }
        when {
            winRateA != null -> WinBar(percentA = winRateA, sample = sample)
            tokenGated -> TokenGatedBanner(
                feature = stringResource(R.string.winrate_feature),
                instructions = stringResource(R.string.winrate_instructions),
            )
        }
    }
}

@Composable
private fun CharCol(charId: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PortraitOrb(charId = charId, size = 56.dp, contentDescription = charId.replaceFirstChar { it.uppercase() })
        Text(charId.replaceFirstChar { it.uppercase() }, color = SmatchupColors.Text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TabBar(selected: MatchupDetailTab, onSelect: (MatchupDetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SmatchupColors.Bg1.copy(alpha = 0.92f))
            .border(1.dp, SmatchupColors.Purple.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState()),
    ) {
        MatchupDetailTab.entries.forEach { tab ->
            Text(
                text = stringResource(tab.labelRes).uppercase(),
                color = if (tab == selected) SmatchupColors.Gold else SmatchupColors.TextDim,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { onSelect(tab) }.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}
