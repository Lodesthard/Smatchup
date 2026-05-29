package com.example.smatchup.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
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

    Box(
        modifier = modifier.wolBackground().fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold)
            Text(stringResource(R.string.auth_login_title), style = MaterialTheme.typography.titleLarge, color = SmatchupColors.Text)

            OutlinedTextField(
                value = identifier, onValueChange = { identifier = it },
                label = { Text(stringResource(R.string.auth_field_identifier)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_field_password)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(stringResource(it.messageRes()), color = SmatchupColors.DangerRed) }

            GlowButton(
                text = if (state.isLoading) "..." else stringResource(R.string.auth_login_cta),
                onClick = { viewModel.login(identifier, password) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            GlowButton(
                text = stringResource(R.string.auth_create_account),
                onClick = onGoToRegister,
                variant = GlowButtonVariant.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
