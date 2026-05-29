package com.example.smatchup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smatchup.SmatchupApp
import com.example.smatchup.di.AppContainer
import com.example.smatchup.ui.character.CharacterDetailViewModel
import com.example.smatchup.ui.character.CharacterListViewModel
import com.example.smatchup.ui.home.HomeViewModel
import com.example.smatchup.ui.matchup.MatchupDetailViewModel
import com.example.smatchup.ui.matchup.MatchupPickerViewModel
import com.example.smatchup.ui.tierlist.TierlistViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java          -> HomeViewModel() as T
        CharacterListViewModel::class.java -> CharacterListViewModel(container.characterRepository) as T
        MatchupPickerViewModel::class.java -> MatchupPickerViewModel(container.characterRepository) as T
        TierlistViewModel::class.java      -> TierlistViewModel(container.tierlistRepository) as T
        else -> error("Unknown ViewModel ${modelClass.name}")
    }

    /** Parameterized factory for `MatchupDetailViewModel(charA, charB)`. */
    fun matchupDetail(charA: String, charB: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == MatchupDetailViewModel::class.java)
                return MatchupDetailViewModel(
                    charA = charA,
                    charB = charB,
                    matchupRepo = container.matchupRepository,
                    winrateComputer = container.winrateComputer,
                    bestPlayerRepo = container.bestPlayerRepository,
                    youTubeApi = container.youtubeApi,
                ) as T
            }
        }

    /** Parameterized factory for `CharacterDetailViewModel(charId)`. */
    fun characterDetail(charId: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == CharacterDetailViewModel::class.java)
                return CharacterDetailViewModel(
                    charId = charId,
                    repo = container.characterRepository,
                    bestPlayerRepo = container.bestPlayerRepository,
                    youTubeApi = container.youtubeApi,
                ) as T
            }
        }

    companion object {
        fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
    }
}
