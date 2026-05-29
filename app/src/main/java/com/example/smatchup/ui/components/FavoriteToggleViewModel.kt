package com.example.smatchup.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoriteToggleViewModel(
    private val isFav: suspend (userId: Long) -> Boolean,
    private val toggle: suspend (userId: Long) -> Boolean,
    private val currentUserId: suspend () -> Long?,
) : ViewModel() {

    constructor(authRepo: AuthRepository, favRepo: FavoritesRepository, charId: String) : this(
        isFav = { uid -> favRepo.isCharacterFavorite(uid, charId) },
        toggle = { uid -> favRepo.toggleCharacter(uid, charId) },
        currentUserId = { authRepo.currentUserId() },
    )

    constructor(authRepo: AuthRepository, favRepo: FavoritesRepository, charA: String, charB: String) : this(
        isFav = { uid -> favRepo.isMatchupFavorite(uid, charA, charB) },
        toggle = { uid -> favRepo.toggleMatchup(uid, charA, charB) },
        currentUserId = { authRepo.currentUserId() },
    )

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = currentUserId() ?: return@launch
            _isFavorite.value = isFav(uid)
        }
    }

    fun toggle() {
        viewModelScope.launch {
            val uid = currentUserId() ?: return@launch
            _isFavorite.value = toggle(uid)
        }
    }
}
