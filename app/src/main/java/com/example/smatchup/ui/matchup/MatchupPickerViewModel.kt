package com.example.smatchup.ui.matchup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchupPickerUiState(
    val isLoading: Boolean = true,
    val roster: List<Character> = emptyList(),
    val query: String = "",
    val visible: List<Character> = emptyList(),
    val slotA: String? = null,
    val slotB: String? = null,
    val ready: Pair<String, String>? = null,
)

class MatchupPickerViewModel(
    private val loadRoster: suspend () -> List<Character>,
) : ViewModel() {

    constructor(repo: CharacterRepository) : this(loadRoster = { repo.loadRoster() })

    private val _state = MutableStateFlow(MatchupPickerUiState())
    val state: StateFlow<MatchupPickerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val r = try { loadRoster() } catch (_: Throwable) { emptyList() }
            _state.update { s -> s.copy(isLoading = false, roster = r, visible = applyFilter(r, s.query)) }
        }
    }

    fun setQuery(q: String) {
        _state.update { s -> s.copy(query = q, visible = applyFilter(s.roster, q)) }
    }

    private fun applyFilter(roster: List<Character>, query: String): List<Character> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return roster
        return roster.filter { it.name.lowercase().contains(q) }
    }

    fun pick(charId: String) {
        _state.update { s ->
            when {
                s.slotA == null -> s.copy(slotA = charId)
                s.slotA == charId -> s
                s.slotB == null -> s.copy(slotB = charId, ready = s.slotA to charId)
                else -> s.copy(slotA = charId, slotB = null, ready = null)
            }
        }
    }

    fun reset() {
        _state.update { it.copy(slotA = null, slotB = null, ready = null) }
    }
}
