package com.example.smatchup.ui.tierlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.TierlistRepository
import com.example.smatchup.domain.model.TierlistView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TierlistUiState(
    val isLoading: Boolean = true,
    val selectedName: String = "strength",
    val view: TierlistView? = null,
    val error: String? = null,
)

class TierlistViewModel(
    private val load: suspend (name: String) -> TierlistView,
) : ViewModel() {

    constructor(repo: TierlistRepository) : this(load = { repo.load(it) })

    private val _state = MutableStateFlow(TierlistUiState())
    val state: StateFlow<TierlistUiState> = _state.asStateFlow()

    init { reload() }

    fun select(name: String) {
        _state.update { it.copy(selectedName = name) }
        reload()
    }

    private fun reload() {
        val name = _state.value.selectedName
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val v = load(name)
                _state.update { it.copy(isLoading = false, view = v) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
