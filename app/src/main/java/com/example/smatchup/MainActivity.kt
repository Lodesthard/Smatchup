package com.example.smatchup

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.theme.SmatchupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmatchupTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    SearchScreen(
                        modifier = Modifier
                            .padding(padding)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Smatchup — VOD VGBootCamp",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = vm.character1,
            onValueChange = { vm.character1 = it },
            label = { Text("Personnage 1 (optionnel, ex : Mario)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.character2,
            onValueChange = { vm.character2 = it },
            label = { Text("Personnage 2 (optionnel)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.player1,
            onValueChange = { vm.player1 = it },
            label = { Text("Joueur 1 (optionnel, ex : Glutonny)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.player2,
            onValueChange = { vm.player2 = it },
            label = { Text("Joueur 2 (optionnel)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.search() },
            enabled = vm.state !is SearchState.Loading &&
                listOf(vm.character1, vm.character2, vm.player1, vm.player2)
                    .any { it.isNotBlank() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Rechercher la VOD la plus récente") }

        when (val s = vm.state) {
            SearchState.Idle -> {}
            SearchState.Loading -> CircularProgressIndicator()
            SearchState.Empty -> Text("Aucune VOD trouvée.")
            is SearchState.Error -> Text(
                "Erreur : ${s.message}",
                color = MaterialTheme.colorScheme.error
            )
            is SearchState.Success -> {
                Text(s.title, style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, s.url.toUri())
                        )
                    }
                ) { Text(s.url) }
            }
        }
    }
}
