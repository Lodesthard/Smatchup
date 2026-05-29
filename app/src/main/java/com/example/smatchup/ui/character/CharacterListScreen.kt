package com.example.smatchup.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.R
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.OrbCard
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun CharacterListScreen(
    onCharacterClick: (charId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterListViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.wolBackground().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.roster_title),
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
            modifier = Modifier.padding(top = 24.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(SmatchupColors.Bg2)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = state.query,
                onValueChange = { viewModel.setQuery(it) },
                singleLine = true,
                textStyle = TextStyle(color = SmatchupColors.Text, fontSize = 16.sp),
                cursorBrush = SolidColor(SmatchupColors.Gold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (state.query.isEmpty()) {
                        Text(stringResource(R.string.search_hint), color = SmatchupColors.TextDim)
                    }
                    inner()
                },
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingOrb()
            }
            state.error != null -> EmptyState(
                message = stringResource(R.string.error_loading, state.error ?: ""),
                ctaText = stringResource(R.string.retry),
                onCta = { viewModel.reload() },
            )
            state.visible.isEmpty() -> EmptyState(message = stringResource(R.string.no_results, state.query))
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                contentPadding = PaddingValues(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = state.visible, key = { it.id }) { c ->
                    OrbCard(
                        label = c.name,
                        charId = c.id,
                        modifier = Modifier,
                        onClick = { onCharacterClick(c.id) },
                    )
                }
            }
        }
    }
}
