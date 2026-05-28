package com.example.smatchup.ui.matchup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.api.YouTubeApi
import com.example.smatchup.data.repository.BestPlayerRepository
import com.example.smatchup.data.repository.MatchupRepository
import com.example.smatchup.data.winrate.WinrateComputer
import com.example.smatchup.data.winrate.WinrateResult
import com.example.smatchup.domain.model.ApiResult
import com.example.smatchup.domain.model.Matchup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchupDetailUiState(
    val isLoading: Boolean = true,
    val matchup: Matchup? = null,
    val winRateA: Float? = null,
    val winrateSample: Int = 0,
    val winrateTokenGated: Boolean = false,
    val lastVideoUrl: String? = null,
    val lastVideoTitle: String? = null,
    val selectedTab: MatchupDetailTab = MatchupDetailTab.GAMEPLAN,
    val error: String? = null,
)

class MatchupDetailViewModel(
    private val charA: String,
    private val charB: String,
    private val loadMatchup: suspend () -> Matchup,
    private val loadWinrate: suspend () -> ApiResult<WinrateResult>,
    private val loadVideo: suspend () -> YouTubeApi.Video?,
) : ViewModel() {

    constructor(
        charA: String,
        charB: String,
        matchupRepo: MatchupRepository,
        winrateComputer: WinrateComputer,
        bestPlayerRepo: BestPlayerRepository,
        youTubeApi: YouTubeApi,
    ) : this(
        charA = charA,
        charB = charB,
        loadMatchup = { matchupRepo.getMatchup(charA, charB) },
        loadWinrate = { winrateComputer.winrate(charA, charB) },
        loadVideo = {
            val pa = bestPlayerRepo.bestPlayerFor(charA)?.tag
            val pb = bestPlayerRepo.bestPlayerFor(charB)?.tag
            val terms = listOfNotNull(pa, pb).ifEmpty { listOf(charA, charB) }
            when (val r = youTubeApi.searchLatest(terms)) {
                is ApiResult.Success -> r.data
                else -> null
            }
        },
    )

    private val _state = MutableStateFlow(MatchupDetailUiState())
    val state: StateFlow<MatchupDetailUiState> = _state.asStateFlow()

    init { reload() }

    fun selectTab(tab: MatchupDetailTab) = _state.update { it.copy(selectedTab = tab) }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val mu = loadMatchup()
                _state.update { it.copy(isLoading = false, matchup = mu) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }
        }
        viewModelScope.launch {
            when (val r = loadWinrate()) {
                is ApiResult.Success -> _state.update {
                    it.copy(winRateA = r.data.winRateA, winrateSample = r.data.sampleSize)
                }
                ApiResult.Unauthorized -> _state.update { it.copy(winrateTokenGated = true) }
                else -> { /* leave null */ }
            }
        }
        viewModelScope.launch {
            val video = try { loadVideo() } catch (_: Throwable) { null }
            _state.update { it.copy(lastVideoUrl = video?.url, lastVideoTitle = video?.title) }
        }
    }
}
