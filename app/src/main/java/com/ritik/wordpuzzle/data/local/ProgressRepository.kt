package com.ritik.wordpuzzle.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Progress for one category that must outlive the process. */
data class CategoryProgress(
    /** Highest position unlocked in this category. The first position is always available. */
    val highestUnlockedOrder: Int = 1,
    /** Ids of levels the player has completed. */
    val completedLevels: Set<Int> = emptySet(),
    /** Total bonus words discovered in this category. */
    val totalBonusWords: Int = 0,
)

/** Kept as a source-compatible name for code that only handles one category. */
typealias PlayerProgress = CategoryProgress

/**
 * Durable progress store.
 *
 * Backed by Preferences DataStore: writes are transactional and off the main
 * thread, and reads are exposed as a [Flow] so the UI re-renders when progress
 * changes without any manual refresh.
 */
interface ProgressRepository {
    fun progress(categoryId: String): Flow<CategoryProgress>
    val allProgress: Flow<Map<String, CategoryProgress>>
    suspend fun completeLevel(
        categoryId: String,
        levelId: Int,
        nextLevelOrder: Int?,
        bonusWordsFound: Int,
    )
    suspend fun resetProgress(categoryId: String)
}

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_progress",
)

class DataStoreProgressRepository private constructor(
    private val dataStore: DataStore<Preferences>,
) : ProgressRepository {

    constructor(context: Context) : this(context.progressDataStore)

    internal constructor(dataStore: DataStore<Preferences>, testOnly: Unit = Unit) : this(dataStore)

    override fun progress(categoryId: String): Flow<CategoryProgress> {
        requireCategoryId(categoryId)
        return flow {
            migrateLegacyProgressIfNeeded()
            emitAll(dataStore.data.map { it.readProgress(categoryId) })
        }
    }

    override val allProgress: Flow<Map<String, CategoryProgress>> = flow {
        migrateLegacyProgressIfNeeded()
        emitAll(
            dataStore.data.map { prefs ->
                prefs[KEY_CATEGORIES].orEmpty().associateWith { categoryId ->
                    prefs.readProgress(categoryId)
                }
            }
        )
    }

    override suspend fun completeLevel(
        categoryId: String,
        levelId: Int,
        nextLevelOrder: Int?,
        bonusWordsFound: Int,
    ) {
        requireCategoryId(categoryId)
        dataStore.edit { prefs ->
            prefs.migrateLegacyProgressIfNeeded()
            prefs.registerCategory(categoryId)

            val completedKey = completedKey(categoryId)
            val completed = prefs[completedKey].orEmpty().toMutableSet()
            val isFirstCompletion = completed.add(levelId.toString())
            prefs[completedKey] = completed

            // Completion and its bonus award are one transaction. Replaying an
            // already completed level cannot award the same bonus words again.
            if (isFirstCompletion && bonusWordsFound > 0) {
                val bonusKey = bonusTotalKey(categoryId)
                prefs[bonusKey] = (prefs[bonusKey] ?: 0) + bonusWordsFound
            }

            // Unlock only moves forward: replaying an old level must never lock
            // the player out of progress they already earned.
            val highestKey = highestUnlockedKey(categoryId)
            val current = prefs[highestKey] ?: 1
            if (nextLevelOrder != null && nextLevelOrder > current) {
                prefs[highestKey] = nextLevelOrder
            }
        }
    }

    override suspend fun resetProgress(categoryId: String) {
        requireCategoryId(categoryId)
        dataStore.edit { prefs ->
            prefs.remove(highestUnlockedKey(categoryId))
            prefs.remove(completedKey(categoryId))
            prefs.remove(bonusTotalKey(categoryId))
            prefs[KEY_CATEGORIES] = prefs[KEY_CATEGORIES].orEmpty() - categoryId
        }
    }

    private suspend fun migrateLegacyProgressIfNeeded() {
        dataStore.edit { it.migrateLegacyProgressIfNeeded() }
    }

    private fun Preferences.readProgress(categoryId: String) = CategoryProgress(
        highestUnlockedOrder = this[highestUnlockedKey(categoryId)] ?: 1,
        completedLevels = this[completedKey(categoryId)]
            ?.mapNotNull(String::toIntOrNull)
            ?.toSet()
            .orEmpty(),
        totalBonusWords = this[bonusTotalKey(categoryId)] ?: 0,
    )

    private fun MutablePreferences.migrateLegacyProgressIfNeeded() {
        if (this[KEY_LEGACY_CATEGORY] != null) return

        val hasLegacyProgress = this[LEGACY_HIGHEST_UNLOCKED] != null ||
            this[LEGACY_COMPLETED] != null ||
            this[LEGACY_BONUS_TOTAL] != null

        if (hasLegacyProgress) {
            // The old catalogue had no category identity. Only its first two
            // positions have an explicit mapping to schema v1's first category.
            // Never fan a legacy global unlock across all new categories.
            this[LEGACY_HIGHEST_UNLOCKED]?.let {
                this[highestUnlockedKey(LEGACY_MIGRATION_CATEGORY)] = it.coerceIn(1, 2)
            }
            this[LEGACY_COMPLETED]?.let { old ->
                this[completedKey(LEGACY_MIGRATION_CATEGORY)] = old
                    .mapNotNull(String::toIntOrNull)
                    .filter { it in 1..2 }
                    .map(Int::toString)
                    .toSet()
            }
            this[LEGACY_BONUS_TOTAL]?.let {
                this[bonusTotalKey(LEGACY_MIGRATION_CATEGORY)] = it.coerceAtLeast(0)
            }
            registerCategory(LEGACY_MIGRATION_CATEGORY)
        }

        // Write a marker even for a fresh install so legacy data restored later
        // cannot unexpectedly replace category progress.
        this[KEY_LEGACY_CATEGORY] = LEGACY_MIGRATION_CATEGORY
    }

    private fun MutablePreferences.registerCategory(categoryId: String) {
        this[KEY_CATEGORIES] = this[KEY_CATEGORIES].orEmpty() + categoryId
    }

    private companion object {
        val KEY_CATEGORIES = stringSetPreferencesKey("progress_category_ids")
        val KEY_LEGACY_CATEGORY = stringPreferencesKey("legacy_progress_category")

        val LEGACY_HIGHEST_UNLOCKED = intPreferencesKey("highest_unlocked_level")
        val LEGACY_COMPLETED = stringSetPreferencesKey("completed_levels")
        val LEGACY_BONUS_TOTAL = intPreferencesKey("total_bonus_words")
        const val LEGACY_MIGRATION_CATEGORY = "animals"

        fun highestUnlockedKey(categoryId: String) =
            intPreferencesKey("category_${encodeCategoryId(categoryId)}_highest_unlocked_level")

        fun completedKey(categoryId: String) =
            stringSetPreferencesKey("category_${encodeCategoryId(categoryId)}_completed_levels")

        fun bonusTotalKey(categoryId: String) =
            intPreferencesKey("category_${encodeCategoryId(categoryId)}_total_bonus_words")

        fun requireCategoryId(categoryId: String) {
            require(categoryId.isNotBlank()) { "categoryId must not be blank" }
        }

        /** UTF-8 hex is deterministic and cannot collide with key separators. */
        fun encodeCategoryId(categoryId: String): String = categoryId
            .encodeToByteArray()
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
