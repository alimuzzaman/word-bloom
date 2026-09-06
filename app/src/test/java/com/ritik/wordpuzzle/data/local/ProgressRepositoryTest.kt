package com.ritik.wordpuzzle.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRepositoryTest {

    @Test
    fun `categories keep independent completion and bonus progress`() = runTest {
        val repository = repository()

        repository.completeLevel("animals", levelId = 1, nextLevelOrder = 2, bonusWordsFound = 3)
        repository.completeLevel("food-and-drink", levelId = 4, nextLevelOrder = 5, bonusWordsFound = 0)

        assertEquals(
            CategoryProgress(2, setOf(1), 3),
            repository.progress("animals").first(),
        )
        assertEquals(
            CategoryProgress(5, setOf(4), 0),
            repository.progress("food-and-drink").first(),
        )
    }

    @Test
    fun `replaying a completed level does not award its bonus twice`() = runTest {
        val repository = repository()

        repository.completeLevel("animals", levelId = 1, nextLevelOrder = 2, bonusWordsFound = 3)
        repository.completeLevel("animals", levelId = 1, nextLevelOrder = 2, bonusWordsFound = 3)

        assertEquals(
            CategoryProgress(2, setOf(1), 3),
            repository.progress("animals").first(),
        )
    }

    @Test
    fun `ten levels unlock in order and completion stays bounded to one category`() = runTest {
        val repository = repository()

        // A fresh category starts at Level 1. Each completion unlocks only the
        // following position; the final level has no next position to unlock.
        assertEquals(CategoryProgress(), repository.progress("fish").first())
        (1..10).forEach { levelId ->
            val nextOrder = (levelId + 1).takeIf { it <= 10 }
            repository.completeLevel(
                categoryId = "fish",
                levelId = levelId,
                nextLevelOrder = nextOrder,
                bonusWordsFound = 1,
            )
            val progress = repository.progress("fish").first()
            assertEquals(
                (levelId + 1).coerceAtMost(10),
                progress.highestUnlockedOrder,
            )
            assertEquals((1..levelId).toSet(), progress.completedLevels)
        }

        // Completing Fish must not unlock or mark an unrelated category.
        assertEquals(CategoryProgress(), repository.progress("birds").first())
        assertEquals(10, repository.progress("fish").first().highestUnlockedOrder)
        assertEquals(10, repository.progress("fish").first().totalBonusWords)
    }

    @Test
    fun `replaying an earlier level never relocks a ten-level category`() = runTest {
        val repository = repository()

        (1..10).forEach { levelId ->
            repository.completeLevel(
                categoryId = "plants",
                levelId = levelId,
                nextLevelOrder = (levelId + 1).takeIf { it <= 10 },
                bonusWordsFound = 0,
            )
        }
        repository.completeLevel(
            categoryId = "plants",
            levelId = 2,
            nextLevelOrder = 3,
            bonusWordsFound = 99,
        )

        val progress = repository.progress("plants").first()
        assertEquals(10, progress.highestUnlockedOrder)
        assertEquals((1..10).toSet(), progress.completedLevels)
        assertEquals(0, progress.totalBonusWords)
    }

    @Test
    fun `legacy global progress migrates only valid positions to the explicit first category`() = runTest {
        val dataStore = FakePreferencesDataStore(
            preferencesOf(
                intPreferencesKey("highest_unlocked_level") to 7,
                stringSetPreferencesKey("completed_levels") to setOf("1", "2", "bad"),
                intPreferencesKey("total_bonus_words") to 11,
            ),
        )
        val repository = DataStoreProgressRepository(dataStore)

        assertEquals(
            CategoryProgress(2, setOf(1, 2), 11),
            repository.progress("animals").first(),
        )
        assertEquals(CategoryProgress(), repository.progress("travel").first())
    }

    @Test
    fun `all progress lists registered categories and reset removes only its target`() = runTest {
        val repository = repository()
        repository.completeLevel("animals", 1, 2, 2)
        repository.completeLevel("travel", 1, 2, 5)

        assertEquals(setOf("animals", "travel"), repository.allProgress.first().keys)

        repository.resetProgress("animals")

        assertEquals(CategoryProgress(), repository.progress("animals").first())
        assertEquals(
            CategoryProgress(2, setOf(1), 5),
            repository.progress("travel").first(),
        )
    }

    @Test
    fun `story reading mode defaults to compact and survives a preference write`() = runTest {
        val repository = DataStoreStoryReadingModeRepository(FakePreferencesDataStore())

        assertEquals(StoryReadingMode.COMPACT, repository.mode.first())

        repository.setMode(StoryReadingMode.CHAPTERS)

        assertEquals(StoryReadingMode.CHAPTERS, repository.mode.first())
    }

    private fun repository() = DataStoreProgressRepository(FakePreferencesDataStore())
}

private class FakePreferencesDataStore(
    initial: Preferences = preferencesOf(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = transform(state.value).also { state.value = it }
}
