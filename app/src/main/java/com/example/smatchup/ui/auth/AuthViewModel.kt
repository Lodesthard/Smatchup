package com.example.smatchup.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val loggedIn: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val doLogin: suspend (identifier: String, password: String) -> AuthResult,
    private val doRegister: suspend (pseudo: String, email: String, password: String) -> AuthResult,
) : ViewModel() {

    constructor(repo: AuthRepository) : this(
        doLogin = { id, pw -> repo.login(id, pw) },
        doRegister = { p, e, pw -> repo.register(p, e, pw) },
    )

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Remplis tous les champs") }
            return
        }
        submit { doLogin(identifier, password) }
    }

    fun register(pseudo: String, email: String, password: String, confirm: String) {
        if (pseudo.isBlank() || email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Remplis tous les champs") }; return
        }
        if (!email.contains("@")) { _state.update { it.copy(error = "Email invalide") }; return }
        if (password.length < 6) { _state.update { it.copy(error = "Mot de passe trop court (min 6)") }; return }
        if (password != confirm) { _state.update { it.copy(error = "Les mots de passe ne correspondent pas") }; return }
        submit { doRegister(pseudo, email, password) }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    private fun submit(block: suspend () -> AuthResult) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val res = block()) {
                is AuthResult.Success -> _state.update { it.copy(isLoading = false, loggedIn = true) }
                is AuthResult.Failure -> _state.update { it.copy(isLoading = false, error = res.reason) }
            }
        }
    }
}
