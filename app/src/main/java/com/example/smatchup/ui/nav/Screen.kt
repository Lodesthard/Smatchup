package com.example.smatchup.ui.nav

sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object CharacterList  : Screen("characters")

    object CharacterDetail : Screen("char/{charId}") {
        const val ARG_CHAR_ID = "charId"
        fun buildRoute(charId: String): String = "char/$charId"
    }

    object MatchupPicker  : Screen("mu/pick")
    object MatchupDetail  : Screen("mu/{charA}/{charB}") {
        const val ARG_CHAR_A = "charA"
        const val ARG_CHAR_B = "charB"
        fun buildRoute(charA: String, charB: String): String = "mu/$charA/$charB"
    }
    object Tierlist       : Screen("tiers")
    object Favorites      : Screen("favorites")
    object Profile        : Screen("profile")
}
