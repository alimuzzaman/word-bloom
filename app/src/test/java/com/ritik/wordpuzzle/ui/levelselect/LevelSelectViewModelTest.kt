package com.ritik.wordpuzzle.ui.levelselect

import com.ritik.wordpuzzle.data.local.CategoryProgress
import com.ritik.wordpuzzle.data.local.PlayerProgress
import com.ritik.wordpuzzle.data.local.ProgressRepository
import com.ritik.wordpuzzle.data.repository.LevelRepository
import com.ritik.wordpuzzle.domain.model.Category
import com.ritik.wordpuzzle.domain.model.Direction
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.domain.model.LetterTile
import com.ritik.wordpuzzle.domain.model.Placement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

private const val TEST_CATEGORY = "fish"

@OptIn(ExperimentalCoroutinesApi::class)
class LevelSelectViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var levelRepository: TenLevelRepository
    private lateinit var progressRepository: InMemoryProgressRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        levelRepository = TenLevelRepository()
        progressRepository = InMemoryProgressRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a fresh category exposes ten levels with only the first unlocked`() = runTest(dispatcher) {
        val viewModel = LevelSelectViewModel(TEST_CATEGORY, levelRepository, progressRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(10, state.levels.size)
        assertEquals(listOf(1), state.levels.filter { it.isUnlocked }.map { it.id })
        assertEquals(emptyList<Int>(), state.levels.filter { it.isCompleted }.map { it.id })
    }

    @Test
    fun `completing each level unlocks only the next level through level ten`() = runTest(dispatcher) {
        val viewModel = LevelSelectViewModel(TEST_CATEGORY, levelRepository, progressRepository)
        advanceUntilIdle()

        (1..10).forEach { levelId ->
            progressRepository.completeLevel(
                categoryId = TEST_CATEGORY,
                levelId = levelId,
                nextLevelOrder = (levelId + 1).takeIf { it <= 10 },
                bonusWordsFound = 0,
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(levelId, state.completedCount)
            assertEquals(
                (1..(levelId + 1).coerceAtMost(10)).toList(),
                state.levels.filter { it.isUnlocked }.map { it.id },
            )
            assertEquals((1..levelId).toList(), state.levels.filter { it.isCompleted }.map { it.id })
        }
    }

    @Test
    fun `progress in one category does not unlock another category`() = runTest(dispatcher) {
        val fish = LevelSelectViewModel(TEST_CATEGORY, levelRepository, progressRepository)
        val birds = LevelSelectViewModel("birds", levelRepository, progressRepository)
        advanceUntilIdle()

        progressRepository.completeLevel(TEST_CATEGORY, levelId = 1, nextLevelOrder = 2, bonusWordsFound = 0)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), fish.state.value.levels.filter { it.isUnlocked }.map { it.id })
        assertEquals(listOf(1), birds.state.value.levels.filter { it.isUnlocked }.map { it.id })
    }
}

private class TenLevelRepository : LevelRepository {
    private val categories = listOf(
        Category(TEST_CATEGORY, "Fish", "মাছ", "fish", 1),
        Category("birds", "Birds", "পাখি", "bird", 2),
    )
    private val levelsByCategory = categories.associate { category ->
        category.id to (1..10).map { id ->
            Level(
                id = id,
                categoryId = category.id,
                order = id,
                letterTiles = listOf(LetterTile(0, 'A'), LetterTile(1, 'B'), LetterTile(2, 'C')),
                words = listOf("ABC"),
                bonusWords = emptySet(),
                rows = 1,
                cols = 3,
                placements = listOf(Placement("ABC", 0, 0, Direction.HORIZONTAL)),
            )
        }
    }

    override suspend fun getCategories(): List<Category> = categories

    override suspend fun getLevels(categoryId: String): List<Level> = levelsByCategory[categoryId].orEmpty()

    override suspend fun getLevel(categoryId: String, id: Int): Level? =
        getLevels(categoryId).firstOrNull { it.id == id }
}

private class InMemoryProgressRepository : ProgressRepository {
    private val states = mutableMapOf<String, MutableStateFlow<CategoryProgress>>()
    private val allProgressState = MutableStateFlow<Map<String, PlayerProgress>>(emptyMap())

    override fun progress(categoryId: String): Flow<PlayerProgress> = stateFor(categoryId)

    override val allProgress: Flow<Map<String, PlayerProgress>> = allProgressState

    override suspend fun completeLevel(
        categoryId: String,
        levelId: Int,
        nextLevelOrder: Int?,
        bonusWordsFound: Int,
    ) {
        val state = stateFor(categoryId)
        val current = state.value
        val isFirstCompletion = levelId !in current.completedLevels
        state.value = current.copy(
            highestUnlockedOrder = maxOf(current.highestUnlockedOrder, nextLevelOrder ?: current.highestUnlockedOrder),
            completedLevels = current.completedLevels + levelId,
            totalBonusWords = current.totalBonusWords + if (isFirstCompletion) bonusWordsFound else 0,
        )
        allProgressState.value = states.mapValues { it.value.value }
    }

    override suspend fun resetProgress(categoryId: String) {
        states.remove(categoryId)
        allProgressState.value = states.mapValues { it.value.value }
    }

    private fun stateFor(categoryId: String): MutableStateFlow<CategoryProgress> =
        states.getOrPut(categoryId) { MutableStateFlow(CategoryProgress()) }
}
