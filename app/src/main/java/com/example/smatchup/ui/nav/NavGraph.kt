package com.example.smatchup.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.character.CharacterDetailScreen
import com.example.smatchup.ui.character.CharacterListScreen
import com.example.smatchup.ui.home.HomeRoutes
import com.example.smatchup.ui.home.HomeScreen
import com.example.smatchup.ui.matchup.MatchupDetailScreen
import com.example.smatchup.ui.matchup.MatchupPickerScreen
import com.example.smatchup.ui.auth.LoginScreen
import com.example.smatchup.ui.auth.RegisterScreen
import com.example.smatchup.ui.favorites.FavoritesScreen
import com.example.smatchup.ui.profile.ProfileScreen
import com.example.smatchup.ui.splash.SplashScreen
import com.example.smatchup.ui.tierlist.TierlistScreen
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun SmatchupNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
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

@Composable
private fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier.wolBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = SmatchupColors.Gold,
        )
    }
}
