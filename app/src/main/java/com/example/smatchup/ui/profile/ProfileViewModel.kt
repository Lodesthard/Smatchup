package com.example.smatchup.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val pseudo: String = "",
    val email: String = "",
    val loggedOut: Boolean = false,
)

class ProfileViewModel(private val authRepo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val u = authRepo.currentUser()
            _state.value = ProfileUiState(
                isLoading = false,
                pseudo = u?.pseudo.orEmpty(),
                email = u?.email.orEmpty(),
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _state.value = _state.value.copy(loggedOut = true)
        }
    }
}
