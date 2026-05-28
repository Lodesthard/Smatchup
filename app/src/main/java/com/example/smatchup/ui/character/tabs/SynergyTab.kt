package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.OrbCard

@Composable
fun SynergyTab(
    partners: List<Character>,
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (partners.isEmpty()) {
        EmptyState(message = "Pas encore de synergies définies.", modifier = modifier)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = partners, key = { it.id }) { c ->
            OrbCard(label = c.name, onClick = { onCharacterClick(c.id) })
        }
    }
}
