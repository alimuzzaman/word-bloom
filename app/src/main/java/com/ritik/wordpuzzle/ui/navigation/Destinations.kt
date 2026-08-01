package com.ritik.wordpuzzle.ui.navigation

/**
 * Type-safe route definitions.
 *
 * Routes are built through [Game.createRoute] rather than string-concatenated at
 * call sites, so an argument can never be omitted or ordered wrongly.
 */
sealed class Destination(val route: String) {

    data object Splash : Destination("splash")

    data object Home : Destination("home")

    data object LevelSelect : Destination("level_select")

    data object Game : Destination("game/{levelId}") {
        const val ARG_LEVEL_ID = "levelId"
        fun createRoute(levelId: Int) = "game/$levelId"
    }
}
