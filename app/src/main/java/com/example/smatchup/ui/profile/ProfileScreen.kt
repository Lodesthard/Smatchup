package com.example.smatchup.ui.profile

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
import com.example.smatchup.i18n.LocaleManager
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.GlowButton
import com.example.smatchup.ui.components.GlowButtonVariant
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val currentLang = LocaleManager.getLanguage(context)

    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    fun changeLanguage(lang: String) {
        if (lang == currentLang) return
        LocaleManager.setLanguage(context, lang)
        (context as? Activity)?.recreate()
    }

    Box(
        modifier = modifier.wolBackground().fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
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
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold, modifier = Modifier.padding(top = 24.dp))
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingOrb() }
            } else {
                Text(state.pseudo, style = MaterialTheme.typography.titleLarge, color = SmatchupColors.Text)
                Text(state.email, style = MaterialTheme.typography.bodyMedium, color = SmatchupColors.TextDim)

                LanguageSelector(currentLang = currentLang, onSelect = ::changeLanguage)

                GlowButton(
                    text = stringResource(R.string.profile_logout),
                    onClick = viewModel::logout,
                    variant = GlowButtonVariant.GHOST,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(currentLang: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.profile_language), style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Purple)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlowButton(
                text = stringResource(R.string.language_english),
                onClick = { onSelect(LocaleManager.EN) },
                variant = if (currentLang == LocaleManager.EN) GlowButtonVariant.PRIMARY else GlowButtonVariant.GHOST,
                modifier = Modifier.weight(1f),
            )
            GlowButton(
                text = stringResource(R.string.language_french),
                onClick = { onSelect(LocaleManager.FR) },
                variant = if (currentLang == LocaleManager.FR) GlowButtonVariant.PRIMARY else GlowButtonVariant.GHOST,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
