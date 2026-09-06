package com.ritik.wordpuzzle.data.repository

import com.ritik.wordpuzzle.data.model.CategoryDto
import com.ritik.wordpuzzle.data.model.LevelCatalogDto
import com.ritik.wordpuzzle.data.model.LevelDto
import com.ritik.wordpuzzle.domain.model.Category
import com.ritik.wordpuzzle.domain.model.GridPosition
import kotlinx.serialization.json.Json

/**
 * Parsed and validated catalogue.  The parser is shared by the bundled asset and
 * the network source so a server response cannot bypass the same puzzle checks.
 */
internal data class LevelCatalog(
    val catalogVersion: String,
    val categories: List<Category>,
    val levelsByCategory: Map<String, List<com.ritik.wordpuzzle.domain.model.Level>>,
)

/**
 * Validates the wire contract before exposing any level to the game.
 *
 * Remote content is untrusted input.  In particular, checking only JSON syntax is
 * not enough: a malformed placement or wheel could make a level impossible to
 * complete.  Keep these checks independent of Android so they are easy to test.
 */
internal class LevelCatalogParser(
    private val json: Json,
    /** Network content must be educator-approved; bundled content may be draft. */
    private val requireApprovedContent: Boolean = false,
) {

    fun parse(raw: String): LevelCatalog {
        val dto = json.decodeFromString<LevelCatalogDto>(raw)
        require(dto.schemaVersion in SUPPORTED_SCHEMA_VERSIONS) {
            "Unsupported category schema ${dto.schemaVersion}"
        }
        require(validCatalogVersion(dto.catalogVersion)) {
            "Invalid catalog version '${dto.catalogVersion}'"
        }
        require(dto.categories.isNotEmpty()) { "Catalog must contain a category" }
        require(dto.levels.isEmpty()) { "Schema stores levels inside categories" }
        require(dto.categories.map { it.id }.distinct().size == dto.categories.size) {
            "Duplicate category id"
        }
        require(dto.categories.map { it.order }.distinct().size == dto.categories.size) {
            "Duplicate category order"
        }
        require(dto.categories.map { it.order }.sorted() == (1..dto.categories.size).toList()) {
            "Category order must be contiguous from 1"
        }

        dto.categories.forEach { category -> validateCategory(category, dto.schemaVersion) }

        val categories = dto.categories
            .sortedBy { it.order }
            .map {
                Category(
                    id = it.id,
                    nameEn = it.nameEn,
                    nameBn = it.nameBn,
                    iconKey = it.iconKey,
                    order = it.order,
                    introductionEn = it.introductionEn,
                    introductionBn = it.introductionBn,
                    completionEn = it.completionEn,
                    completionBn = it.completionBn,
                    completionWords = it.completionWords,
                    storyEn = it.storyEn,
                    storyBn = it.storyBn,
                    storyWords = it.storyWords,
                    contentReviewStatus = it.contentReviewStatus,
                )
            }
        val allLevels = dto.categories.flatMap { category -> category.levels.map { it.toDomain() } }
        return LevelCatalog(
            catalogVersion = dto.catalogVersion,
            categories = categories,
            levelsByCategory = allLevels.groupBy { it.categoryId }
                .mapValues { (_, levels) -> levels.sortedBy { it.order } },
        )
    }

    private fun validateCategory(category: CategoryDto, schemaVersion: Int) {
        require(category.id.isNotBlank()) { "Category id must not be blank" }
        require(category.nameEn.isNotBlank() && category.nameBn.isNotBlank()) {
            "Category ${category.id} must have bilingual names"
        }
        require(category.iconKey.isNotBlank()) { "Category ${category.id} must have an icon key" }
        if (schemaVersion >= CONTENT_SCHEMA_VERSION) {
            require(category.introductionEn.isNotBlank() && category.introductionBn.isNotBlank()) {
                "Category ${category.id} must have bilingual introduction text"
            }
            require(category.completionEn.isNotBlank() && category.completionBn.isNotBlank()) {
                "Category ${category.id} must have bilingual completion text"
            }
            require(category.storyEn.isNotBlank() && category.storyBn.isNotBlank()) {
                "Category ${category.id} must have a bilingual story"
            }
            require(category.contentReviewStatus.isNotBlank()) {
                "Category ${category.id} content review status is blank"
            }
            if (requireApprovedContent) {
                require(category.contentReviewStatus == APPROVED_REVIEW_STATUS) {
                    "Category ${category.id} content is not approved"
                }
            }
            require(category.completionWords.isNotEmpty()) {
                "Category ${category.id} must list completion words"
            }
        }
        require(category.levels.isNotEmpty()) { "Category ${category.id} has no levels" }
        require(category.levels.map { it.id }.distinct().size == category.levels.size) {
            "Duplicate level id in ${category.id}"
        }
        require(category.levels.map { it.order }.distinct().size == category.levels.size) {
            "Duplicate level order in ${category.id}"
        }
        require(category.levels.map { it.order }.sorted() == (1..category.levels.size).toList()) {
            "Level order in ${category.id} must be contiguous from 1"
        }
        val orderedDifficulties = category.levels.sortedBy { it.order }.map { it.difficulty }
        require(orderedDifficulties.zipWithNext().all { (current, next) -> next >= current }) {
            "Difficulty in ${category.id} must not decrease"
        }

        category.levels.forEach { level -> validateLevel(category, level, schemaVersion) }

        if (schemaVersion >= CONTENT_SCHEMA_VERSION) {
            val heroes = category.levels.mapNotNull { it.heroWord?.word?.uppercase() }.toSet()
            val completionWords = category.completionWords.map(String::uppercase).toSet()
            require(completionWords.isNotEmpty() && completionWords.all { it in heroes }) {
                "Category ${category.id} completion words must be learned hero words"
            }

            val targetWords = category.levels
                .flatMap { level -> level.words.map(String::uppercase) }
                .toSet()
            val storyWords = category.storyWords.map(String::uppercase)
            require(storyWords.isNotEmpty() && storyWords.toSet() == targetWords &&
                storyWords.size == targetWords.size) {
                "Category ${category.id} story words must cover every target word exactly once"
            }
            require(storyWords.all { storyContainsWord(category.storyEn, it) }) {
                "Category ${category.id} story must contain every story word"
            }
        }
    }

    private fun validateLevel(category: CategoryDto, level: LevelDto, schemaVersion: Int) {
        val prefix = "Level ${category.id}:${level.id}"
        require(level.categoryId == category.id) {
            "$prefix category ${level.categoryId} does not match ${category.id}"
        }
        require(level.id > 0 && level.order > 0) { "$prefix ids and order must be positive" }
        require(level.difficulty in 1..5) { "$prefix difficulty must be 1..5" }
        require(level.letters.isNotBlank()) { "$prefix wheel must not be blank" }
        require(level.words.isNotEmpty()) { "$prefix must have target words" }
        val words = level.words.map(String::uppercase)
        require(words.size == words.toSet().size) { "$prefix target words must be unique" }
        require(words.all { it.isNotBlank() && it.all(Char::isLetter) }) {
            "$prefix target words must contain letters only"
        }
        val wheel = level.letters.uppercase().toList()
        require(wheel.all(Char::isLetter)) { "$prefix wheel must contain letters only" }
        require(words.all { canSpell(it, wheel) }) { "$prefix has an unspellable target word" }
        val longest = words.maxByOrNull(String::length).orEmpty()
        require(longest.toList().sorted() == wheel.sorted()) {
            "$prefix wheel must be an anagram of longest word '$longest'"
        }

        val bonuses = level.bonusWords.map(String::uppercase)
        require(bonuses.size == bonuses.toSet().size) { "$prefix bonus words must be unique" }
        require(bonuses.none { it in words }) { "$prefix bonus word duplicates a target" }
        require(bonuses.all { it.isNotBlank() && it.all(Char::isLetter) && canSpell(it, wheel) }) {
            "$prefix contains an invalid bonus word"
        }
        require(level.rows in 1..20 && level.cols in 1..20) {
            "$prefix grid dimensions are invalid"
        }
        require(level.placements.map { it.word.uppercase() }.toSet() == words.toSet()) {
            "$prefix placement words must match target words"
        }
        val board = mutableMapOf<GridPosition, Char>()
        level.placements.forEach { placement ->
            val word = placement.word.uppercase()
            require(placement.direction.equals("HORIZONTAL", ignoreCase = true) ||
                placement.direction.equals("VERTICAL", ignoreCase = true)) {
                "$prefix has an invalid placement direction"
            }
            require(placement.row >= 0 && placement.col >= 0) {
                "$prefix has a negative placement origin"
            }
            val dr = if (placement.direction.equals("VERTICAL", ignoreCase = true)) 1 else 0
            val dc = if (dr == 0) 1 else 0
            word.forEachIndexed { index, ch ->
                val cell = GridPosition(placement.row + dr * index, placement.col + dc * index)
                require(cell.row in 0 until level.rows && cell.col in 0 until level.cols) {
                    "$prefix '$word' leaves the grid at $cell"
                }
                val existing = board[cell]
                require(existing == null || existing == ch) {
                    "$prefix crossing conflict at $cell"
                }
                board[cell] = ch
            }
        }

        val hero = requireNotNull(level.heroWord) { "$prefix must have bilingual hero metadata" }
        require(hero.word.isNotBlank() && hero.definitionEn.isNotBlank() &&
            hero.translationBn.isNotBlank() && hero.meaningBn.isNotBlank()) {
            "$prefix has incomplete hero metadata"
        }
        require(hero.word.uppercase() in words) { "$prefix hero word must be a target word" }

        if (schemaVersion >= CONTENT_SCHEMA_VERSION) {
            require(level.ageBand.isNotBlank()) { "$prefix age band is blank" }
            require(level.wordMeanings.size == words.size) {
                "$prefix must define every target word"
            }
            val meaningWords = level.wordMeanings.map { it.word.uppercase() }
            require(meaningWords.toSet() == words.toSet()) {
                "$prefix meanings must cover target words exactly"
            }
            level.wordMeanings.forEach { meaning ->
                require(meaning.word.isNotBlank() && meaning.definitionEn.isNotBlank() &&
                    meaning.translationBn.isNotBlank() && meaning.meaningBn.isNotBlank()) {
                    "$prefix contains incomplete word meaning"
                }
            }
            val lesson = requireNotNull(level.lesson) { "$prefix lesson is missing" }
            require(lesson.sentenceEn.isNotBlank() && lesson.sentenceBn.isNotBlank()) {
                "$prefix lesson must be bilingual"
            }
            val usedWords = lesson.usedWords.map(String::uppercase)
            require(usedWords.size == words.size && usedWords.toSet() == words.toSet()) {
                "$prefix lesson usedWords must cover every target word exactly once"
            }
            require(words.all { storyContainsWord(lesson.sentenceEn, it) }) {
                "$prefix English lesson must contain every target word"
            }
        }
    }

    private fun canSpell(word: String, letters: List<Char>): Boolean {
        val pool = letters.groupingBy { it }.eachCount().toMutableMap()
        for (ch in word) {
            val remaining = pool[ch] ?: return false
            if (remaining == 0) return false
            pool[ch] = remaining - 1
        }
        return true
    }

    private fun validCatalogVersion(version: String): Boolean =
        version.length <= MAX_VERSION_LENGTH && VERSION_PATTERN.matches(version)

    /**
     * Match an English target as a standalone word, allowing punctuation or
     * whitespace around it but not a longer alphabetic word containing it.
     */
    private fun storyContainsWord(story: String, word: String): Boolean =
        Regex("(?i)(?<![A-Za-z])${Regex.escape(word)}(?![A-Za-z])").containsMatchIn(story)

    private companion object {
        val SUPPORTED_SCHEMA_VERSIONS = setOf(1, 2)
        const val CONTENT_SCHEMA_VERSION = 2
        const val APPROVED_REVIEW_STATUS = "approved"
        const val MAX_VERSION_LENGTH = 64
        // Schema 1 reference servers derive a content hash when the authored
        // version is absent; schema 2 publishers use numeric date/revision
        // versions such as 2026.09.02-1.
        val VERSION_PATTERN = Regex("(?:[0-9]+(?:[._-][0-9A-Za-z]+)*|sha256-[0-9A-Fa-f]{64})")
    }
}
