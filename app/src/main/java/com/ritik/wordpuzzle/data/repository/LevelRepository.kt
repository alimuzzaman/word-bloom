package com.ritik.wordpuzzle.data.repository

import android.content.Context
import com.ritik.wordpuzzle.data.model.LevelDto
import com.ritik.wordpuzzle.domain.model.Category
import com.ritik.wordpuzzle.domain.model.Direction
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.domain.model.HeroWord
import com.ritik.wordpuzzle.domain.model.LetterTile
import com.ritik.wordpuzzle.domain.model.Placement
import com.ritik.wordpuzzle.domain.model.LevelLesson
import com.ritik.wordpuzzle.domain.model.WordMeaning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Source of the level catalogue.
 *
 * Declared as an interface so the ViewModel depends on an abstraction rather than
 * on asset loading — tests substitute a fake without touching Android APIs.
 */
interface LevelRepository {
    suspend fun getCategories(): List<Category>
    suspend fun getLevels(categoryId: String): List<Level>
    suspend fun getLevel(categoryId: String, id: Int): Level?
    suspend fun levelCount(categoryId: String): Int = getLevels(categoryId).size
    suspend fun getNextLevel(categoryId: String, id: Int): Level? {
        val levels = getLevels(categoryId)
        val index = levels.indexOfFirst { it.id == id }
        return if (index >= 0) levels.getOrNull(index + 1) else null
    }
}

/**
 * Loads categories and their levels from `assets/categories.json`.
 *
 * The parse happens once and is memoised; [mutex] makes the lazy init safe when
 * several coroutines request levels concurrently (e.g. level-select and gameplay
 * composing at the same time).
 */
class AssetLevelRepository(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : LevelRepository {

    private val mutex = Mutex()
    @Volatile private var cache: LevelCatalog? = null

    private suspend fun catalog(): LevelCatalog {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: loadFromAssets().also { cache = it }
        }
    }

    override suspend fun getCategories(): List<Category> = catalog().categories

    override suspend fun getLevels(categoryId: String): List<Level> =
        catalog().levelsByCategory[categoryId].orEmpty()

    override suspend fun getLevel(categoryId: String, id: Int): Level? =
        getLevels(categoryId).firstOrNull { it.id == id }

    internal suspend fun loadFromAssets(): LevelCatalog = withContext(Dispatchers.IO) {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        LevelCatalogParser(json).parse(raw)
    }

    private companion object {
        const val ASSET_NAME = "categories.json"
    }
}

/**
 * DTO → domain mapping.
 *
 * Words and letters are upper-cased here so every downstream comparison can assume
 * a single casing, and tile ids are assigned by index to survive shuffling.
 */
internal fun LevelDto.toDomain(): Level = Level(
    id = id,
    letterTiles = letters.uppercase().mapIndexed { index, c -> LetterTile(id = index, char = c) },
    words = words.map { it.uppercase() },
    bonusWords = bonusWords.mapTo(mutableSetOf()) { it.uppercase() },
    rows = rows,
    cols = cols,
    placements = placements.map { p ->
        Placement(
            word = p.word.uppercase(),
            row = p.row,
            col = p.col,
            direction = when (p.direction.uppercase()) {
                "VERTICAL" -> Direction.VERTICAL
                else -> Direction.HORIZONTAL
            },
        )
    },
    categoryId = categoryId,
    order = order,
    difficulty = difficulty,
    heroWord = heroWord?.let {
        HeroWord(
            word = it.word.uppercase(),
            definitionEn = it.definitionEn,
            translationBn = it.translationBn,
            meaningBn = it.meaningBn,
        )
    } ?: HeroWord(word = words.maxByOrNull { it.length }?.uppercase().orEmpty()),
    ageBand = ageBand,
    wordMeanings = wordMeanings.map {
        WordMeaning(
            word = it.word.uppercase(),
            definitionEn = it.definitionEn,
            translationBn = it.translationBn,
            meaningBn = it.meaningBn,
        )
    },
    lesson = lesson?.let {
        LevelLesson(
            sentenceEn = it.sentenceEn,
            sentenceBn = it.sentenceBn,
            usedWords = it.usedWords.map(String::uppercase),
        )
    },
)
