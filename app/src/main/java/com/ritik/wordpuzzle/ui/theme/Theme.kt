package com.ritik.wordpuzzle.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Access to the game palette from any composable, e.g. `WordPuzzleTheme.colors.trail`.
 *
 * `staticCompositionLocalOf` is correct here because the palette never changes at
 * runtime — a mutable local would add needless recomposition bookkeeping.
 */
val LocalWordPuzzleColors = staticCompositionLocalOf { WordPuzzleColors() }

/**
 * The game deliberately renders in one dark treatment regardless of system setting:
 * a puzzle board that flips to a light theme mid-session would break the visual
 * identity, and the palette is authored for contrast against the dark backdrop.
 */
@Composable
fun WordPuzzleTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val gameColors = WordPuzzleColors()

    val colorScheme = darkColorScheme(
        primary = AccentAmber,
        onPrimary = InkOnLight,
        secondary = AccentTeal,
        onSecondary = InkOnLight,
        background = BrandDeep,
        onBackground = PaperWhite,
        surface = BrandSurface,
        onSurface = PaperWhite,
        surfaceVariant = BrandSurfaceElevated,
        onSurfaceVariant = PaperMuted,
        error = AccentCoral,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Draw behind the system bars; screens apply their own insets padding.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalWordPuzzleColors provides gameColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WordPuzzleTypography,
            shapes = WordPuzzleShapes,
            content = content,
        )
    }
}

/** Convenience accessor mirroring `MaterialTheme.colorScheme`. */
object WordPuzzleTheme {
    val colors: WordPuzzleColors
        @Composable get() = LocalWordPuzzleColors.current
}
