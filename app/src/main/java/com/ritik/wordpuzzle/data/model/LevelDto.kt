package com.ritik.wordpuzzle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of `assets/levels.json`.
 *
 * These DTOs mirror the JSON exactly and stay separate from the domain models in
 * `domain.model`. Keeping the two apart means a change to the asset schema does not
 * ripple into game logic — only [com.ritik.wordpuzzle.data.repository.LevelMapper]
 * has to adapt.
 */
@Serializable
data class LevelCatalogDto(
    @SerialName("levels") val levels: List<LevelDto>,
)

@Serializable
data class LevelDto(
    @SerialName("id") val id: Int,
    /** Letters on the wheel; always an anagram of the level's longest word. */
    @SerialName("letters") val letters: String,
    /** Words that occupy the crossword grid and must all be found to advance. */
    @SerialName("words") val words: List<String>,
    /** Valid extra words that score but are not on the grid. */
    @SerialName("bonusWords") val bonusWords: List<String>,
    @SerialName("rows") val rows: Int,
    @SerialName("cols") val cols: Int,
    @SerialName("placements") val placements: List<PlacementDto>,
)

@Serializable
data class PlacementDto(
    @SerialName("word") val word: String,
    /** Zero-based grid coordinates of the word's first letter. */
    @SerialName("row") val row: Int,
    @SerialName("col") val col: Int,
    @SerialName("direction") val direction: String,
)
