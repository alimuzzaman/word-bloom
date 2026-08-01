package com.ritik.wordpuzzle.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Player progress that must outlive the process. */
data class PlayerProgress(
    /** Highest level unlocked, 1-based. Level 1 is always available. */
    val highestUnlockedLevel: Int = 1,
    /** Ids of levels the player has completed. */
    val completedLevels: Set<Int> = emptySet(),
    /** Total bonus words discovered across all levels. */
    val totalBonusWords: Int = 0,
)

/**
 * Durable progress store.
 *
 * Backed by Preferences DataStore: writes are transactional and off the main
 * thread, and reads are exposed as a [Flow] so the UI re-renders when progress
 * changes without any manual refresh.
 */
interface ProgressRepository {
    val progress: Flow<PlayerProgress>
    suspend fun markLevelCompleted(levelId: Int, nextLevelId: Int?)
    suspend fun addBonusWords(count: Int)
    suspend fun resetProgress()
}

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_progress",
)

class DataStoreProgressRepository(private val context: Context) : ProgressRepository {

    override val progress: Flow<PlayerProgress> =
        context.progressDataStore.data.map { prefs ->
            PlayerProgress(
                highestUnlockedLevel = prefs[KEY_HIGHEST_UNLOCKED] ?: 1,
                completedLevels = prefs[KEY_COMPLETED]
                    ?.mapNotNull(String::toIntOrNull)
                    ?.toSet()
                    .orEmpty(),
                totalBonusWords = prefs[KEY_BONUS_TOTAL] ?: 0,
            )
        }

    override suspend fun markLevelCompleted(levelId: Int, nextLevelId: Int?) {
        context.progressDataStore.edit { prefs ->
            val completed = prefs[KEY_COMPLETED].orEmpty().toMutableSet()
            completed += levelId.toString()
            prefs[KEY_COMPLETED] = completed

            // Unlock only moves forward: replaying an old level must never lock
            // the player out of progress they already earned.
            val current = prefs[KEY_HIGHEST_UNLOCKED] ?: 1
            if (nextLevelId != null && nextLevelId > current) {
                prefs[KEY_HIGHEST_UNLOCKED] = nextLevelId
            }
        }
    }

    override suspend fun addBonusWords(count: Int) {
        if (count <= 0) return
        context.progressDataStore.edit { prefs ->
            prefs[KEY_BONUS_TOTAL] = (prefs[KEY_BONUS_TOTAL] ?: 0) + count
        }
    }

    override suspend fun resetProgress() {
        context.progressDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_HIGHEST_UNLOCKED = intPreferencesKey("highest_unlocked_level")
        val KEY_COMPLETED = stringSetPreferencesKey("completed_levels")
        val KEY_BONUS_TOTAL = intPreferencesKey("total_bonus_words")
    }
}
