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

    data object Categories : Destination("categories")

    data object LevelSelect : Destination("categories/{categoryId}/levels") {
        const val ARG_CATEGORY_ID = "categoryId"
        fun createRoute(categoryId: String) = "categories/$categoryId/levels"
    }

    data object Game : Destination("game/{categoryId}/{levelId}") {
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_LEVEL_ID = "levelId"
        fun createRoute(categoryId: String, levelId: Int) = "game/$categoryId/$levelId"
    }
}
