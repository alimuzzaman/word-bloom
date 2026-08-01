package com.ritik.wordpuzzle.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ritik.wordpuzzle.di.AppContainer
import com.ritik.wordpuzzle.ui.game.GameRoute
import com.ritik.wordpuzzle.ui.home.HomeRoute
import com.ritik.wordpuzzle.ui.levelselect.LevelSelectRoute
import com.ritik.wordpuzzle.ui.splash.SplashRoute

/**
 * Single source of truth for navigation.
 *
 * ## Back-stack rules encoded here
 * - **Splash is popped on exit.** `popUpTo(Splash) { inclusive = true }` means back
 *   from Home exits the app rather than flashing the splash again.
 * - **Home → Game (via Continue) keeps Home underneath**, so back from a level
 *   returns Home — the player never gets stranded.
 * - **Level Select → Game keeps Level Select underneath**, so back returns to the
 *   grid of levels the player was browsing.
 * - **"Next level" replaces the finished level** (`popUpTo` the current game entry,
 *   inclusive) instead of stacking. Without this, finishing ten levels would leave
 *   ten game screens on the stack and back would walk through every one of them.
 * - **`launchSingleTop`** on every navigation prevents a double-tap from pushing
 *   two copies of the same destination.
 *
 * Pause is *not* a destination: it is state inside the game screen. Modelling it as
 * a route would mean the game could be recreated behind the dialog and would
 * complicate back handling; as state, back from Pause is a plain state transition.
 */
@Composable
fun WordPuzzleNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Splash.route,
        modifier = modifier,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {

        composable(
            route = Destination.Splash.route,
            // The splash owns its own animation; a nav transition on top would
            // double-animate it.
            exitTransition = { fadeOut(tween(SPLASH_FADE_MS)) },
        ) {
            SplashRoute(
                onFinished = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Destination.Home.route) {
            HomeRoute(
                appContainer = appContainer,
                onPlay = { levelId ->
                    navController.navigate(Destination.Game.createRoute(levelId)) {
                        launchSingleTop = true
                    }
                },
                onLevelSelect = {
                    navController.navigate(Destination.LevelSelect.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Destination.LevelSelect.route) {
            LevelSelectRoute(
                appContainer = appContainer,
                onLevelClick = { levelId ->
                    navController.navigate(Destination.Game.createRoute(levelId)) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Destination.Game.route,
            arguments = listOf(
                navArgument(Destination.Game.ARG_LEVEL_ID) { type = NavType.IntType },
            ),
            enterTransition = { scaleIn(initialScale = 0.94f, animationSpec = tween(TRANSITION_MS)) + fadeIn(tween(TRANSITION_MS)) },
            popExitTransition = { scaleOut(targetScale = 0.94f, animationSpec = tween(TRANSITION_MS)) + fadeOut(tween(TRANSITION_MS)) },
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt(Destination.Game.ARG_LEVEL_ID) ?: 1
            GameRoute(
                levelId = levelId,
                appContainer = appContainer,
                onNavigateToLevel = { nextLevelId ->
                    navController.navigate(Destination.Game.createRoute(nextLevelId)) {
                        // Replace, don't stack: the finished level must not linger
                        // on the back stack.
                        popUpTo(backStackEntry.destination.route ?: Destination.Game.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToLevelSelect = {
                    navController.navigate(Destination.LevelSelect.route) {
                        // Land on Level Select with Home beneath it, whether the
                        // player entered the game from Home or from Level Select.
                        popUpTo(Destination.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateHome = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

// ---------------------------------------------------------------- transitions
// Horizontal slide with a fade reads as depth: forward pushes left, back pulls right.

private val EASING = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val TRANSITION_MS = 340
private const val SPLASH_FADE_MS = 420

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition =
    slideInHorizontally(tween(TRANSITION_MS, easing = EASING)) { it / 4 } +
        fadeIn(tween(TRANSITION_MS, easing = EASING))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition =
    slideOutHorizontally(tween(TRANSITION_MS, easing = EASING)) { -it / 6 } +
        fadeOut(tween(TRANSITION_MS / 2, easing = EASING))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter(): EnterTransition =
    slideInHorizontally(tween(TRANSITION_MS, easing = EASING)) { -it / 6 } +
        fadeIn(tween(TRANSITION_MS, easing = EASING))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit(): ExitTransition =
    slideOutHorizontally(tween(TRANSITION_MS, easing = EASING)) { it / 4 } +
        fadeOut(tween(TRANSITION_MS / 2, easing = EASING))
