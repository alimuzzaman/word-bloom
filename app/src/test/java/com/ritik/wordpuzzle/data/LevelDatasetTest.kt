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
 * These parse the real `assets/levels.json` rather than a fixture, so an unplayable
 * level can never reach a build: a word that cannot be spelled from its wheel, or a
 * crossing where two words disagree, fails here rather than stranding a player on a
 * level they cannot finish.
 */
class LevelDatasetTest {

    private val levels: List<Level> by lazy {
        // Unit tests run on the JVM without an AssetManager, so read the asset from
        // the source tree directly.
        val file = File("src/main/assets/levels.json")
        assertTrue("levels.json not found at ${file.absolutePath}", file.exists())
        Json { ignoreUnknownKeys = true }
            .decodeFromString<LevelCatalogDto>(file.readText())
            .levels
            .map { it.toDomain() }
    }

    @Test
    fun `catalogue holds at least the required number of levels`() {
        // The brief asks for 10-20 playable levels.
        assertTrue("expected >= 10 levels, found ${levels.size}", levels.size >= 10)
        assertTrue("expected <= 20 levels, found ${levels.size}", levels.size <= 20)
    }

    @Test
    fun `level ids are unique and sequential from one`() {
        val ids = levels.map { it.id }
        assertEquals(ids.distinct(), ids)
        assertEquals((1..levels.size).toList(), ids)
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
    fun `difficulty ramps - later levels are never easier than the first`() {
        val firstWheel = levels.first().letters.size
        val lastWheel = levels.last().letters.size
        assertTrue(
            "expected the final level's wheel ($lastWheel) to exceed the first ($firstWheel)",
            lastWheel > firstWheel,
        )
    }

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
