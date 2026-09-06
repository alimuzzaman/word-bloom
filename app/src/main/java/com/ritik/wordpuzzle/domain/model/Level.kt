package com.ritik.wordpuzzle.domain.model

import androidx.compose.runtime.Immutable

/** Orientation of a word on the crossword grid. */
enum class Direction { HORIZONTAL, VERTICAL }

/** A word placed on the grid at a known origin and orientation. */
@Immutable
data class Placement(
    val word: String,
    val row: Int,
    val col: Int,
    val direction: Direction,
) {
    /** Grid coordinates this word occupies, first letter first. */
    val cells: List<GridPosition> = List(word.length) { i ->
        when (direction) {
            Direction.HORIZONTAL -> GridPosition(row, col + i)
            Direction.VERTICAL -> GridPosition(row + i, col)
        }
    }
}

/** Row/column address of a single crossword cell. */
@Immutable
data class GridPosition(val row: Int, val col: Int)

/**
 * A fully-resolved, playable level.
 *
 * [letterTiles] carries a stable id per tile so the wheel can animate letters
 * through a shuffle: two tiles may share a character (e.g. the two E's in "CASTLE"
 * would collide if we keyed on the character alone).
 */
@Immutable
data class Level(
    val id: Int,
    val letterTiles: List<LetterTile>,
    val words: List<String>,
    val bonusWords: Set<String>,
    val rows: Int,
    val cols: Int,
    val placements: List<Placement>,
    val categoryId: String = "",
    val order: Int = id,
    val difficulty: Int = 1,
    val heroWord: HeroWord = HeroWord(word = words.maxByOrNull { it.length }.orEmpty()),
    /** Optional learning metadata; empty for legacy schema v1 catalogues. */
    val ageBand: String = "",
    val wordMeanings: List<WordMeaning> = emptyList(),
    val lesson: LevelLesson? = null,
) {
    val key: LevelKey = LevelKey(categoryId, id)
    /** Every grid cell that holds a letter, mapped to that letter. */
    val occupiedCells: Map<GridPosition, Char> =
        placements.flatMap { p -> p.cells.zip(p.word.toList()) }.toMap()

    val letters: List<Char> = letterTiles.map { it.char }

    /** Longest word — used for the level-select preview. */
    val keyWord: String = words.maxByOrNull { it.length }.orEmpty()
}

@Immutable
data class LevelKey(val categoryId: String, val levelId: Int)

@Immutable
data class Category(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val iconKey: String,
    val order: Int,
    val introductionEn: String = "",
    val introductionBn: String = "",
    val completionEn: String = "",
    val completionBn: String = "",
    val completionWords: List<String> = emptyList(),
    /** Long bilingual story containing the category's target vocabulary. */
    val storyEn: String = "",
    val storyBn: String = "",
    val storyWords: List<String> = emptyList(),
    val contentReviewStatus: String = "",
)

@Immutable
data class HeroWord(
    val word: String,
    val definitionEn: String = "",
    val translationBn: String = "",
    val meaningBn: String = "",
)

/** Bilingual definition for one playable answer in a level. */
@Immutable
data class WordMeaning(
    val word: String,
    val definitionEn: String,
    val translationBn: String,
    val meaningBn: String,
)

/** Bilingual sentence that reinforces a level's target words. */
@Immutable
data class LevelLesson(
    val sentenceEn: String,
    val sentenceBn: String,
    val usedWords: List<String> = emptyList(),
)

/** One letter on the wheel. [id] is stable across shuffles so animations can track it. */
@Immutable
data class LetterTile(val id: Int, val char: Char)
