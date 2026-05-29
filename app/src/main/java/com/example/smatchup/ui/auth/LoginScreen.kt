package com.example.smatchup.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.GlowButton
import com.example.smatchup.ui.components.GlowButtonVariant
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoggedIn() }

    Column(
        modifier = modifier.wolBackground().fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Smatchup", style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold)
        Text("Connexion", style = MaterialTheme.typography.titleLarge, color = SmatchupColors.Text)

        OutlinedTextField(
            value = identifier, onValueChange = { identifier = it },
            label = { Text("Pseudo ou email") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(it, color = SmatchupColors.DangerRed) }

        GlowButton(
            text = if (state.isLoading) "..." else "Se connecter",
            onClick = { viewModel.login(identifier, password) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        GlowButton(
            text = "Créer un compte",
            onClick = onGoToRegister,
            variant = GlowButtonVariant.GHOST,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
