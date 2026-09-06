package com.ritik.wordpuzzle.data

import com.ritik.wordpuzzle.data.model.LevelCatalogDto
import com.ritik.wordpuzzle.data.repository.toDomain
import com.ritik.wordpuzzle.domain.model.GridPosition
import com.ritik.wordpuzzle.domain.model.Level
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Integrity tests for the shipped level catalogue.
 *
 * These parse the real `assets/categories.json` rather than a fixture, so an unplayable
 * level can never reach a build: a word that cannot be spelled from its wheel, or a
 * crossing where two words disagree, fails here rather than stranding a player on a
 * level they cannot finish.
 */
class LevelDatasetTest {

    private val catalog: LevelCatalogDto by lazy {
        // Unit tests run on the JVM without an AssetManager, so read the asset from
        // the source tree directly.
        val file = File("src/main/assets/categories.json")
        assertTrue("categories.json not found at ${file.absolutePath}", file.exists())
        Json { ignoreUnknownKeys = true }
            .decodeFromString<LevelCatalogDto>(file.readText())
    }

    private val levels: List<Level> by lazy {
        catalog.categories.flatMap { category -> category.levels.map { it.toDomain() } }
    }

    @Test
    fun `catalogue holds sixteen bilingual categories with ten levels each`() {
        assertEquals(2, catalog.schemaVersion)
        assertEquals(16, catalog.categories.size)
        catalog.categories.forEach { category ->
            assertTrue(category.id.isNotBlank())
            assertTrue(category.nameEn.isNotBlank())
            assertTrue(category.nameBn.isNotBlank())
            assertTrue(category.introductionEn.isNotBlank())
            assertTrue(category.introductionBn.isNotBlank())
            assertTrue(category.completionEn.isNotBlank())
            assertTrue(category.completionBn.isNotBlank())
            assertTrue(category.contentReviewStatus.isNotBlank())
            assertEquals(10, category.levels.size)
            assertTrue(category.levels.all { it.categoryId == category.id })
        }
    }

    @Test
    fun `level composite ids and orders are unique within each category`() {
        assertEquals(levels.size, levels.map { it.key }.distinct().size)
        catalog.categories.forEach { category ->
            assertEquals((1..10).toList(), category.levels.sortedBy { it.order }.map { it.order })
        }
    }

    @Test
    fun `category stories cover every target word taught in that category`() {
        catalog.categories.forEach { category ->
            val heroWords = category.levels.mapNotNull { it.heroWord?.word?.uppercase() }.toSet()
            assertTrue("${category.id} must have completion words", category.completionWords.isNotEmpty())
            assertTrue(
                "${category.id} completion words must be learned heroes",
                category.completionWords.map(String::uppercase).all { it in heroWords },
            )
            val targetWords = category.levels
                .flatMap { it.words }
                .map(String::uppercase)
                .toSet()
            assertEquals(
                "${category.id} storyWords must cover every unique target",
                targetWords,
                category.storyWords.map(String::uppercase).toSet(),
            )
            assertEquals(
                "${category.id} storyWords must not contain duplicates",
                targetWords.size,
                category.storyWords.size,
            )
            assertTrue("${category.id} Bengali story is blank", category.storyBn.isNotBlank())
            targetWords.forEach { word ->
                assertTrue(
                    "${category.id} English story omits whole word '$word'",
                    containsWholeWord(category.storyEn, word),
                )
            }
        }
    }

    @Test
    fun `every target word can be spelled from its wheel letters`() {
        levels.forEach { level ->
            level.words.forEach { word ->
                assertTrue(
                    "Level ${level.id}: '$word' is not spellable from ${level.letters.joinToString("")}",
                    canSpell(word, level.letters),
                )
            }
        }
    }

    @Test
    fun `every bonus word can be spelled and is not also a grid word`() {
        levels.forEach { level ->
            level.bonusWords.forEach { word ->
                assertTrue(
                    "Level ${level.id}: bonus '$word' not spellable from ${level.letters.joinToString("")}",
                    canSpell(word, level.letters),
                )
                assertTrue(
                    "Level ${level.id}: bonus '$word' duplicates a grid word",
                    word !in level.words,
                )
            }
        }
    }

    @Test
    fun `the wheel is exactly an anagram of the longest word`() {
        levels.forEach { level ->
            val longest = level.words.maxByOrNull { it.length }.orEmpty()
            assertEquals(
                "Level ${level.id}: wheel does not match longest word '$longest'",
                longest.toList().sorted(),
                level.letters.sorted(),
            )
        }
    }

    @Test
    fun `every word has a placement and every placement stays in bounds`() {
        levels.forEach { level ->
            assertEquals(
                "Level ${level.id}: placement count differs from word count",
                level.words.toSet(),
                level.placements.map { it.word }.toSet(),
            )
            level.placements.forEach { placement ->
                placement.cells.forEach { cell ->
                    assertTrue(
                        "Level ${level.id}: '${placement.word}' leaves the grid at $cell",
                        cell.row in 0 until level.rows && cell.col in 0 until level.cols,
                    )
                }
            }
        }
    }

    @Test
    fun `crossing words agree on the shared letter`() {
        levels.forEach { level ->
            val board = mutableMapOf<GridPosition, Char>()
            level.placements.forEach { placement ->
                placement.cells.zip(placement.word.toList()).forEach { (cell, ch) ->
                    val existing = board[cell]
                    if (existing != null && existing != ch) {
                        fail(
                            "Level ${level.id}: conflict at $cell — " +
                                "'$existing' vs '$ch' from '${placement.word}'",
                        )
                    }
                    board[cell] = ch
                }
            }
        }
    }

    @Test
    fun `grids stay small enough to fit a phone screen`() {
        levels.forEach { level ->
            assertTrue(
                "Level ${level.id} is ${level.rows}x${level.cols}, too large to render",
                level.rows <= 10 && level.cols <= 10,
            )
        }
    }

    @Test
    fun `difficulty rises inside every category`() {
        catalog.categories.forEach { category ->
            val difficulties = category.levels.sortedBy { it.order }.map { it.difficulty }
            assertTrue("${category.id} difficulty must not go backwards", difficulties.zipWithNext().all { (a, b) -> b >= a })
            assertTrue("${category.id} must span more than one difficulty", difficulties.first() < difficulties.last())
        }
    }

    @Test
    fun `every level has bilingual word meanings and a lesson sentence`() {
        levels.forEach { level ->
            val dto = catalog.categories
                .first { it.id == level.categoryId }
                .levels
                .first { it.id == level.id }
            val meanings = dto.wordMeanings
            assertEquals("${level.categoryId}:${level.id} meaning count", level.words.size, meanings.size)
            assertEquals(
                "${level.categoryId}:${level.id} meanings must cover every target",
                level.words.toSet(),
                meanings.map { it.word.uppercase() }.toSet(),
            )
            meanings.forEach { meaning ->
                assertTrue("${level.categoryId}:${level.id} meaning word is blank", meaning.word.isNotBlank())
                assertTrue("${level.categoryId}:${level.id} English definition is blank", meaning.definitionEn.isNotBlank())
                assertTrue("${level.categoryId}:${level.id} Bengali translation is blank", meaning.translationBn.isNotBlank())
                assertTrue("${level.categoryId}:${level.id} Bengali meaning is blank", meaning.meaningBn.isNotBlank())
            }

            val lesson = requireNotNull(dto.lesson) { "${level.categoryId}:${level.id} lesson is missing" }
            assertTrue("${level.categoryId}:${level.id} English lesson is blank", lesson.sentenceEn.isNotBlank())
            assertTrue("${level.categoryId}:${level.id} Bengali lesson is blank", lesson.sentenceBn.isNotBlank())
            assertEquals(
                "${level.categoryId}:${level.id} lesson usedWords must cover every target",
                level.words.toSet(),
                lesson.usedWords.map(String::uppercase).toSet(),
            )
            assertEquals(
                "${level.categoryId}:${level.id} lesson usedWords must not contain duplicates",
                level.words.size,
                lesson.usedWords.size,
            )
            level.words.forEach { word ->
                assertTrue(
                    "${level.categoryId}:${level.id} English lesson omits whole word '$word'",
                    containsWholeWord(lesson.sentenceEn, word),
                )
            }
        }
    }

    private fun containsWholeWord(text: String, word: String): Boolean =
        Regex("(?i)(?<![A-Za-z])${Regex.escape(word)}(?![A-Za-z])").containsMatchIn(text)

    /** True when [word] can be built from [letters], consuming each tile at most once. */
    private fun canSpell(word: String, letters: List<Char>): Boolean {
        val pool = letters.groupingBy { it }.eachCount().toMutableMap()
        for (ch in word) {
            val remaining = pool[ch] ?: return false
            if (remaining == 0) return false
            pool[ch] = remaining - 1
        }
        return true
    }
}
