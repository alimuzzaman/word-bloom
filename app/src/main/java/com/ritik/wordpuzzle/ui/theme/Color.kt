package com.ritik.wordpuzzle.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette.
 *
 * A deep blue-night base keeps the bright letter tiles and the amber swipe trail as
 * the only high-chroma elements on screen, so the eye is always drawn to the
 * interactive parts. Semantic aliases live in [WordPuzzleColors].
 */

// Backdrop
val BrandDeep = Color(0xFF11213C)
val BrandMid = Color(0xFF1B3A63)
val BrandSurface = Color(0xFF1E3357)
val BrandSurfaceElevated = Color(0xFF27436E)

// Accent
val AccentAmber = Color(0xFFFFC24B)
val AccentAmberDeep = Color(0xFFF2A413)
val AccentTeal = Color(0xFF3FD4C0)
val AccentCoral = Color(0xFFFF6F61)
val AccentViolet = Color(0xFF9B8CFF)

// Neutrals
val InkOnLight = Color(0xFF13233F)
val PaperWhite = Color(0xFFF4F7FF)
val PaperMuted = Color(0xFFB9C6E0)
val SlotEmpty = Color(0x33FFFFFF)
val SlotEmptyBorder = Color(0x1FFFFFFF)

/**
 * Semantic colour roles used across the game.
 *
 * Grouping them here rather than reaching for raw values at call sites means a
 * re-skin touches one file, and each colour's *purpose* is documented by its name.
 */
data class WordPuzzleColors(
    val backgroundTop: Color = BrandMid,
    val backgroundBottom: Color = BrandDeep,
    val surface: Color = BrandSurface,
    val surfaceElevated: Color = BrandSurfaceElevated,

    val tileFace: Color = PaperWhite,
    val tileText: Color = InkOnLight,
    val tileSelected: Color = AccentAmber,
    val tileSelectedText: Color = InkOnLight,

    val trail: Color = AccentAmber,
    val wheelRing: Color = Color(0x1AFFFFFF),
    val wheelFace: Color = Color(0x14FFFFFF),

    val gridCellEmpty: Color = SlotEmpty,
    val gridCellBorder: Color = SlotEmptyBorder,
    val gridCellFilled: Color = PaperWhite,
    val gridCellText: Color = InkOnLight,
    val gridCellHinted: Color = AccentViolet,

    val feedbackValid: Color = AccentTeal,
    val feedbackBonus: Color = AccentAmber,
    val feedbackInvalid: Color = AccentCoral,
    val feedbackRepeat: Color = PaperMuted,

    val textPrimary: Color = PaperWhite,
    val textSecondary: Color = PaperMuted,
)
