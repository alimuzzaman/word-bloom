package com.ritik.wordpuzzle.data.repository

import android.content.Context
import com.ritik.wordpuzzle.data.model.LevelCatalogDto
import com.ritik.wordpuzzle.data.model.LevelDto
import com.ritik.wordpuzzle.domain.model.Direction
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.domain.model.LetterTile
import com.ritik.wordpuzzle.domain.model.Placement
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
    suspend fun getLevels(): List<Level>
    suspend fun getLevel(id: Int): Level?
    suspend fun levelCount(): Int
}

/**
 * Loads levels from `assets/levels.json`.
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
    @Volatile private var cache: List<Level>? = null

    override suspend fun getLevels(): List<Level> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: loadFromAssets().also { cache = it }
        }
    }

    override suspend fun getLevel(id: Int): Level? = getLevels().firstOrNull { it.id == id }

    override suspend fun levelCount(): Int = getLevels().size

    private suspend fun loadFromAssets(): List<Level> = withContext(Dispatchers.IO) {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        json.decodeFromString<LevelCatalogDto>(raw)
            .levels
            .map { it.toDomain() }
            .sortedBy { it.id }
    }

    private companion object {
        const val ASSET_NAME = "levels.json"
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
)
