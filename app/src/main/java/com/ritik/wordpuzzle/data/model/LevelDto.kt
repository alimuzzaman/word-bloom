package com.ritik.wordpuzzle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of `assets/categories.json`.
 *
 * These DTOs mirror the JSON exactly and stay separate from the domain models in
 * `domain.model`. Keeping the two apart means a change to the asset schema does not
 * ripple into game logic — only [com.ritik.wordpuzzle.data.repository.LevelMapper]
 * has to adapt.
 */
@Serializable
data class LevelCatalogDto(
    @SerialName("schemaVersion") val schemaVersion: Int = 1,
    /** Opaque, monotonically increasing content version, independent of wire schema. */
    @SerialName("catalogVersion") val catalogVersion: String = "1",
    @SerialName("contentReviewNotice") val contentReviewNotice: String = "",
    @SerialName("categories") val categories: List<CategoryDto> = emptyList(),
    @SerialName("levels") val levels: List<LevelDto> = emptyList(),
)

@Serializable
data class CategoryDto(
    @SerialName("id") val id: String,
    @SerialName("nameEn") val nameEn: String,
    @SerialName("nameBn") val nameBn: String,
    @SerialName("iconKey") val iconKey: String,
    @SerialName("order") val order: Int,
    @SerialName("introductionEn") val introductionEn: String = "",
    @SerialName("introductionBn") val introductionBn: String = "",
    @SerialName("completionEn") val completionEn: String = "",
    @SerialName("completionBn") val completionBn: String = "",
    @SerialName("completionWords") val completionWords: List<String> = emptyList(),
    /** Long, child-friendly category story that reinforces every target word. */
    @SerialName("storyEn") val storyEn: String = "",
    @SerialName("storyBn") val storyBn: String = "",
    @SerialName("storyWords") val storyWords: List<String> = emptyList(),
    @SerialName("contentReviewStatus") val contentReviewStatus: String = "",
    @SerialName("levels") val levels: List<LevelDto>,
)

@Serializable
data class HeroWordDto(
    @SerialName("word") val word: String,
    @SerialName("definitionEn") val definitionEn: String,
    @SerialName("translationBn") val translationBn: String,
    @SerialName("meaningBn") val meaningBn: String,
)

@Serializable
data class WordMeaningDto(
    @SerialName("word") val word: String,
    @SerialName("definitionEn") val definitionEn: String,
    @SerialName("translationBn") val translationBn: String,
    @SerialName("meaningBn") val meaningBn: String,
)

@Serializable
data class LevelLessonDto(
    @SerialName("sentenceEn") val sentenceEn: String,
    @SerialName("sentenceBn") val sentenceBn: String,
    @SerialName("usedWords") val usedWords: List<String> = emptyList(),
)

@Serializable
data class LevelDto(
    @SerialName("id") val id: Int,
    @SerialName("categoryId") val categoryId: String = "",
    @SerialName("order") val order: Int = id,
    @SerialName("difficulty") val difficulty: Int = 1,
    @SerialName("ageBand") val ageBand: String = "",
    @SerialName("heroWord") val heroWord: HeroWordDto? = null,
    @SerialName("wordMeanings") val wordMeanings: List<WordMeaningDto> = emptyList(),
    @SerialName("lesson") val lesson: LevelLessonDto? = null,
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
