package com.example.smatchup.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun FavoriteHeart(
    viewModel: FavoriteToggleViewModel,
    modifier: Modifier = Modifier,
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    IconButton(onClick = viewModel::toggle, modifier = modifier) {
        if (isFavorite) {
            Icon(Icons.Filled.Favorite, contentDescription = "Retirer des favoris", tint = SmatchupColors.Gold)
        } else {
            Icon(Icons.Filled.FavoriteBorder, contentDescription = "Ajouter aux favoris", tint = SmatchupColors.TextDim)
        }
    }
}
