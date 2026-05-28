package com.example.smatchup.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.api.YouTubeApi
import com.example.smatchup.data.repository.BestPlayerRepository
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.domain.model.ApiResult
import com.example.smatchup.domain.model.CharacterDetail
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterDetailUiState(
    val isLoading: Boolean = true,
    val detail: CharacterDetail? = null,
    val framedata: List<Move> = emptyList(),
    val framedataSource: FramedataSource = FramedataSource.NONE,
    val bestPlayer: Player? = null,
    val lastVideoUrl: String? = null,
    val lastVideoTitle: String? = null,
    val lastVideoTokenGated: Boolean = false,
    val selectedTab: DetailTab = DetailTab.COMBOS,
    val error: String? = null,
)

class CharacterDetailViewModel(
    private val charId: String,
    private val loadDetail: suspend () -> CharacterDetail,
    private val loadFramedata: suspend () -> Pair<List<Move>, FramedataSource>,
    private val loadBestPlayer: suspend () -> Player?,
    private val loadLastVideo: suspend (terms: List<String>) -> YouTubeApi.Video?,
) : ViewModel() {

    constructor(
        charId: String,
        repo: CharacterRepository,
        bestPlayerRepo: BestPlayerRepository,
        youTubeApi: YouTubeApi,
    ) : this(
        charId = charId,
        loadDetail = { repo.getDetail(charId) },
        loadFramedata = { repo.framedataWithFallback(charId) },
        loadBestPlayer = { bestPlayerRepo.bestPlayerFor(charId) },
        loadLastVideo = { terms ->
            when (val r = youTubeApi.searchLatest(terms)) {
                is ApiResult.Success -> r.data
                ApiResult.Unauthorized -> throw IllegalStateException("YOUTUBE_API_KEY missing")
                else -> null
            }
        },
    )

    private val _state = MutableStateFlow(CharacterDetailUiState())
    val state: StateFlow<CharacterDetailUiState> = _state.asStateFlow()

    init { reload() }

    fun selectTab(tab: DetailTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val d = loadDetail()
                _state.update { it.copy(isLoading = false, detail = d) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }
        }
        viewModelScope.launch {
            try {
                val (frames, source) = loadFramedata()
                _state.update { it.copy(framedata = frames, framedataSource = source) }
            } catch (_: Throwable) { /* leave NONE */ }
        }
        viewModelScope.launch {
            val player = try { loadBestPlayer() } catch (_: Throwable) { null }
            _state.update { it.copy(bestPlayer = player) }
            if (player == null) return@launch
            try {
                val video = loadLastVideo(listOf(player.tag, charId))
                _state.update {
                    it.copy(
                        lastVideoUrl = video?.url,
                        lastVideoTitle = video?.title,
                    )
                }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("YOUTUBE_API_KEY") == true) {
                    _state.update { it.copy(lastVideoTokenGated = true) }
                }
            } catch (_: Throwable) { /* silent */ }
        }
    }
}
