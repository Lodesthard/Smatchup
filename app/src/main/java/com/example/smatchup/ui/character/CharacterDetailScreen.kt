package com.example.smatchup.ui.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.FavoriteHeart
import com.example.smatchup.ui.components.FavoriteToggleViewModel
import com.example.smatchup.ui.character.tabs.CombosTab
import com.example.smatchup.ui.character.tabs.FrameTab
import com.example.smatchup.ui.character.tabs.GameplanTab
import com.example.smatchup.ui.character.tabs.MovesTab
import com.example.smatchup.ui.character.tabs.StagesTab
import com.example.smatchup.ui.character.tabs.SynergyTab
import com.example.smatchup.ui.character.tabs.VideoTab
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun CharacterDetailScreen(
    charId: String,
    onCharacterClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterDetailViewModel = viewModel(
        factory = ViewModelFactory.fromApp(),
        key = "character-detail-$charId",
    ),
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.wolBackground()) {
        when {
            state.isLoading && state.detail == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            }
            state.error != null -> EmptyState(
                message = "Erreur : ${state.error}",
                ctaText = "Retour",
                onCta = onBack,
            )
            state.detail != null -> {
                val detail = state.detail!!
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailHero(character = detail.character)
                    DetailTabBar(selected = state.selectedTab, onSelect = viewModel::selectTab)
                    when (state.selectedTab) {
                        DetailTab.COMBOS   -> CombosTab(combos = detail.combos)
                        DetailTab.GAMEPLAN -> GameplanTab(gameplan = detail.gameplan)
                        DetailTab.MOVES    -> MovesTab(movesUtility = detail.movesUtility)
                        DetailTab.FRAME    -> FrameTab(moves = state.framedata, source = state.framedataSource)
                        DetailTab.SYNERGY  -> SynergyTab(partners = detail.synergyPartners, onCharacterClick = onCharacterClick)
                        DetailTab.STAGES   -> StagesTab(ban = detail.stagesBan, counterpick = detail.stagesCounterpick)
                        DetailTab.VIDEO    -> VideoTab(
                            videoUrl = state.lastVideoUrl,
                            videoTitle = state.lastVideoTitle,
                            tokenGated = state.lastVideoTokenGated,
                        )
                    }
                }
            }
        }

        if (state.detail != null) {
            FavoriteHeart(
                viewModel = viewModel<FavoriteToggleViewModel>(
                    factory = ViewModelFactory.fromApp().favoriteCharacter(charId),
                    key = "fav-char-$charId",
                ),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            )
        }
    }
}
