package com.example.smatchup.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smatchup.R
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.auth.LoginScreen
import com.example.smatchup.ui.auth.RegisterScreen
import com.example.smatchup.ui.character.CharacterDetailScreen
import com.example.smatchup.ui.character.CharacterListScreen
import com.example.smatchup.ui.favorites.FavoritesScreen
import com.example.smatchup.ui.home.HomeRoutes
import com.example.smatchup.ui.home.HomeScreen
import com.example.smatchup.ui.matchup.MatchupDetailScreen
import com.example.smatchup.ui.matchup.MatchupPickerScreen
import com.example.smatchup.ui.profile.ProfileScreen
import com.example.smatchup.ui.splash.SplashScreen
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.tierlist.TierlistScreen

private data class BottomDest(val route: String, val icon: ImageVector, val labelRes: Int)

private val bottomDestinations = listOf(
    BottomDest(Screen.Home.route, Icons.Filled.Home, R.string.nav_home),
    BottomDest(Screen.CharacterList.route, Icons.Filled.Person, R.string.nav_characters),
    BottomDest(Screen.MatchupPicker.route, Icons.Filled.PlayArrow, R.string.nav_matchup),
    BottomDest(Screen.Tierlist.route, Icons.Filled.List, R.string.nav_tierlists),
    BottomDest(Screen.Favorites.route, Icons.Filled.Favorite, R.string.nav_favorites),
)

// Routes that show the bottom navigation bar (top-level destinations).
private val barRoutes = setOf(
    Screen.Home.route,
    Screen.CharacterList.route,
    Screen.MatchupPicker.route,
    Screen.Tierlist.route,
    Screen.Favorites.route,
    Screen.Profile.route,
)

@Composable
fun SmatchupNavGraph() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = SmatchupColors.Bg1,
        bottomBar = {
            if (currentRoute in barRoutes) {
                NavigationBar(containerColor = SmatchupColors.Bg2) {
                    bottomDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    nav.navigate(dest.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                            label = { Text(stringResource(dest.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmatchupColors.Bg1,
                                selectedTextColor = SmatchupColors.Gold,
                                indicatorColor = SmatchupColors.Gold,
                                unselectedIconColor = SmatchupColors.TextDim,
                                unselectedTextColor = SmatchupColors.TextDim,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            // §8.6 page transitions: fade + subtle slide-up on enter, fade on exit.
            enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 16 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 16 } },
        ) {

            composable(
                Screen.Splash.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
            ) {
                SplashScreen(onResolved = { loggedIn ->
                    val dest = if (loggedIn) Screen.Home.route else Screen.Login.route
                    nav.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    routes = HomeRoutes(
                        onCharacters = { nav.navigate(Screen.CharacterList.route) },
                        onMatchup    = { nav.navigate(Screen.MatchupPicker.route) },
                        onTierlist   = { nav.navigate(Screen.Tierlist.route) },
                        onFavorites  = { nav.navigate(Screen.Favorites.route) },
                        onProfile    = { nav.navigate(Screen.Profile.route) },
                        onLastMatch  = { nav.navigate(Screen.CharacterList.route) },
                    ),
                )
            }

            composable(Screen.CharacterList.route) {
                CharacterListScreen(onCharacterClick = { charId ->
                    nav.navigate(Screen.CharacterDetail.buildRoute(charId))
                })
            }

            composable(
                route = Screen.CharacterDetail.route,
                arguments = listOf(navArgument(Screen.CharacterDetail.ARG_CHAR_ID) { type = NavType.StringType }),
            ) { backStackEntry ->
                val charId = backStackEntry.arguments?.getString(Screen.CharacterDetail.ARG_CHAR_ID).orEmpty()
                CharacterDetailScreen(
                    charId = charId,
                    onCharacterClick = { otherId -> nav.navigate(Screen.CharacterDetail.buildRoute(otherId)) },
                    onBack = { nav.popBackStack() },
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = ViewModelFactory.fromApp().characterDetail(charId),
                        key = "character-detail-$charId",
                    ),
                )
            }
            composable(Screen.MatchupPicker.route) {
                MatchupPickerScreen(onPairReady = { a, b ->
                    nav.navigate(Screen.MatchupDetail.buildRoute(a, b))
                })
            }
            composable(
                route = Screen.MatchupDetail.route,
                arguments = listOf(
                    navArgument(Screen.MatchupDetail.ARG_CHAR_A) { type = NavType.StringType },
                    navArgument(Screen.MatchupDetail.ARG_CHAR_B) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val a = backStackEntry.arguments?.getString(Screen.MatchupDetail.ARG_CHAR_A).orEmpty()
                val b = backStackEntry.arguments?.getString(Screen.MatchupDetail.ARG_CHAR_B).orEmpty()
                MatchupDetailScreen(
                    onBack = { nav.popBackStack() },
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = ViewModelFactory.fromApp().matchupDetail(a, b),
                        key = "matchup-detail-$a-$b",
                    ),
                )
            }
            composable(Screen.Tierlist.route) {
                TierlistScreen(onCharacterClick = { charId ->
                    nav.navigate(Screen.CharacterDetail.buildRoute(charId))
                })
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onCharacterClick = { charId -> nav.navigate(Screen.CharacterDetail.buildRoute(charId)) },
                    onMatchupClick = { a, b -> nav.navigate(Screen.MatchupDetail.buildRoute(a, b)) },
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLoggedOut = { nav.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoggedIn = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                    onGoToRegister = { nav.navigate(Screen.Register.route) },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegistered = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                    onGoToLogin = { nav.popBackStack() },
                )
            }
        }
    }
}
