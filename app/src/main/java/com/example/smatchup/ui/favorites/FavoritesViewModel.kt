package com.example.smatchup.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.data.repository.FavoritesRepository
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val characters: List<Character> = emptyList(),
    val matchups: List<Pair<Character, Character>> = emptyList(),
)

class FavoritesViewModel(
    private val authRepo: AuthRepository,
    private val favRepo: FavoritesRepository,
    private val characterRepo: CharacterRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authRepo.currentUserId() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val byId = characterRepo.loadRoster().associateBy { it.id }
            combine(
                favRepo.observeCharacterIds(uid),
                favRepo.observeMatchupPairs(uid),
            ) { charIds, pairs ->
                FavoritesUiState(
                    isLoading = false,
                    characters = charIds.mapNotNull { byId[it] },
                    matchups = pairs.mapNotNull { (a, b) ->
                        val ca = byId[a]; val cb = byId[b]
                        if (ca != null && cb != null) ca to cb else null
                    },
                )
            }.collect { merged -> _state.value = merged }
        }
    }
}
