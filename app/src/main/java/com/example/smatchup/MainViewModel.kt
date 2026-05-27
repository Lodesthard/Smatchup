package com.example.smatchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.api.YouTubeApi
import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data object Empty : SearchState
    data class Success(val url: String, val title: String) : SearchState
    data class Error(val message: String) : SearchState
}

class MainViewModel : ViewModel() {
    var character1 by mutableStateOf("")
    var character2 by mutableStateOf("")
    var player1 by mutableStateOf("")
    var player2 by mutableStateOf("")
    var state by mutableStateOf<SearchState>(SearchState.Idle)
        private set

    fun search() {
        val terms = listOf(character1, character2, player1, player2)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (terms.isEmpty()) return
        state = SearchState.Loading
        viewModelScope.launch {
            val api = YouTubeApi(
                client = OkHttpClient(),
                apiKey = BuildConfig.YOUTUBE_API_KEY,
            )
            state = when (val r = api.searchLatest(terms)) {
                is ApiResult.Success -> {
                    val video = r.data
                    if (video == null) SearchState.Empty
                    else SearchState.Success(video.url, video.title)
                }
                is ApiResult.Unauthorized -> SearchState.Error("Clé API manquante")
                is ApiResult.NetworkError -> SearchState.Error(r.cause.message ?: "erreur réseau")
                else -> SearchState.Error("erreur inconnue")
            }
        }
    }
}
