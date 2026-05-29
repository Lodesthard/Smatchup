package com.example.smatchup.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterListUiState(
    val isLoading: Boolean = true,
    val all: List<Character> = emptyList(),
    val query: String = "",
    val visible: List<Character> = emptyList(),
    val error: String? = null,
)

class CharacterListViewModel(
    private val loader: suspend () -> List<Character>,
) : ViewModel() {

    constructor(repo: CharacterRepository) : this(loader = { repo.loadRoster() })

    private val _state = MutableStateFlow(CharacterListUiState())
    val state: StateFlow<CharacterListUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val roster = loader()
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        all = roster,
                        visible = applyFilter(roster, s.query),
                    )
                }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun setQuery(q: String) {
        _state.update { s ->
            s.copy(
                query = q,
                visible = applyFilter(s.all, q),
            )
        }
    }

    private fun applyFilter(roster: List<Character>, query: String): List<Character> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return roster
        return roster.filter { it.name.lowercase().contains(q) }
    }
}
