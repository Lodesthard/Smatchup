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
import com.example.smatchup.ui.character.CharacterListScreen
import com.example.smatchup.ui.home.HomeRoutes
import com.example.smatchup.ui.home.HomeScreen
import com.example.smatchup.ui.splash.SplashScreen
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun SmatchupNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(onSplashDone = {
                nav.navigate(Screen.Home.route) {
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
            PlaceholderScreen("Character detail — $charId")
        }
        composable(Screen.MatchupPicker.route) { PlaceholderScreen("Match-up picker — sub-project 4") }
        composable(
            route = Screen.MatchupDetail.route,
            arguments = listOf(
                navArgument(Screen.MatchupDetail.ARG_CHAR_A) { type = NavType.StringType },
                navArgument(Screen.MatchupDetail.ARG_CHAR_B) { type = NavType.StringType },
            ),
        ) { PlaceholderScreen("Match-up detail — sub-project 4") }
        composable(Screen.Tierlist.route)  { PlaceholderScreen("Tier lists — sub-project 5") }
        composable(Screen.Favorites.route) { PlaceholderScreen("Favorites — sub-project 6") }
        composable(Screen.Profile.route)   { PlaceholderScreen("Profile — sub-project 6") }
        composable(Screen.Login.route)     { PlaceholderScreen("Login — sub-project 6") }
        composable(Screen.Register.route)  { PlaceholderScreen("Register — sub-project 6") }
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
