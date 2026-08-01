package com.ritik.wordpuzzle.ui.game

import androidx.lifecycle.SavedStateHandle
import com.ritik.wordpuzzle.data.local.PlayerProgress
import com.ritik.wordpuzzle.data.local.ProgressRepository
import com.ritik.wordpuzzle.data.repository.LevelRepository
import com.ritik.wordpuzzle.domain.model.Direction
import com.ritik.wordpuzzle.domain.model.GridPosition
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the gameplay state machine.
 *
 * The ViewModel is exercised through [GameIntent]s only — the same surface the UI
 * uses — so these tests describe player-visible behaviour rather than internals.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var levelRepository: FakeLevelRepository
    private lateinit var progressRepository: FakeProgressRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        levelRepository = FakeLevelRepository()
        progressRepository = FakeProgressRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        GameViewModel(levelRepository, progressRepository, saved)

    // ------------------------------------------------------------------ loading

    @Test
    fun `loading a level populates the board`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.level?.id)
        assertEquals(listOf('C', 'A', 'T'), state.wheelTiles.map { it.char })
        assertEquals(0, state.foundWords.size)
    }

    @Test
    fun `reloading the same level does not wipe progress`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        vm.trace("CAT")
        advanceUntilIdle()
        assertEquals(setOf("CAT"), vm.state.value.foundWords)

        // Simulates the composable re-running LaunchedEffect after a config change.
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        assertEquals(setOf("CAT"), vm.state.value.foundWords)
    }

    // ---------------------------------------------------------------- selection

    @Test
    fun `tracing a grid word marks it found`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        vm.trace("CAT")
        advanceUntilIdle()

        assertEquals(setOf("CAT"), vm.state.value.foundWords)
        assertEquals(WordFeedback.VALID, vm.state.value.feedback)
        assertTrue(vm.state.value.selection.isEmpty())
    }

    @Test
    fun `tracing a bonus word scores separately from grid words`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        vm.trace("TA")
        advanceUntilIdle()

        assertEquals(setOf("TA"), vm.state.value.foundBonusWords)
        assertTrue(vm.state.value.foundWords.isEmpty())
        assertEquals(WordFeedback.BONUS, vm.state.value.feedback)
    }

    @Test
    fun `tracing a non-word is rejected`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        vm.trace("TAC")
        advanceUntilIdle()

        assertTrue(vm.state.value.foundWords.isEmpty())
        assertEquals(WordFeedback.INVALID, vm.state.value.feedback)
    }

    @Test
    fun `re-tracing a found word reports it as already found`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        vm.trace("CAT")
        advanceUntilIdle()
        vm.trace("CAT")
        advanceUntilIdle()

        assertEquals(1, vm.state.value.foundWords.size)
        assertEquals(WordFeedback.ALREADY_FOUND, vm.state.value.feedback)
    }

    @Test
    fun `dragging back onto the previous tile removes the last letter`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        val tiles = vm.state.value.wheelTiles

        vm.onIntent(GameIntent.BeginSelection(tiles[0]))   // C
        vm.onIntent(GameIntent.ExtendSelection(tiles[1]))  // C-A
        vm.onIntent(GameIntent.ExtendSelection(tiles[2]))  // C-A-T
        assertEquals("CAT", vm.state.value.currentWord)

        // Finger slides back onto A: T should drop off.
        vm.onIntent(GameIntent.ExtendSelection(tiles[1]))
        assertEquals("CA", vm.state.value.currentWord)
    }

    @Test
    fun `a tile already in the word cannot be consumed a second time`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        val tiles = vm.state.value.wheelTiles

        vm.onIntent(GameIntent.BeginSelection(tiles[0]))   // C
        vm.onIntent(GameIntent.ExtendSelection(tiles[1]))  // C-A
        vm.onIntent(GameIntent.ExtendSelection(tiles[2]))  // C-A-T

        // Re-entering the *first* tile is not a backtrack (that would be A), and C
        // is already used, so the selection must be left untouched.
        vm.onIntent(GameIntent.ExtendSelection(tiles[0]))

        assertEquals("CAT", vm.state.value.currentWord)
    }

    @Test
    fun `re-entering the current tile is a no-op`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        val tiles = vm.state.value.wheelTiles

        vm.onIntent(GameIntent.BeginSelection(tiles[0]))
        vm.onIntent(GameIntent.ExtendSelection(tiles[1]))
        // A finger resting inside a tile emits move events continuously; those must
        // not disturb the trail.
        repeat(5) { vm.onIntent(GameIntent.ExtendSelection(tiles[1])) }

        assertEquals("CA", vm.state.value.currentWord)
    }

    @Test
    fun `cancelling a gesture clears the selection without validating`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        val tiles = vm.state.value.wheelTiles

        vm.onIntent(GameIntent.BeginSelection(tiles[0]))
        vm.onIntent(GameIntent.ExtendSelection(tiles[1]))
        vm.onIntent(GameIntent.CancelSelection)

        assertTrue(vm.state.value.selection.isEmpty())
        assertNull(vm.state.value.feedback)
        assertTrue(vm.state.value.foundWords.isEmpty())
    }

    // ------------------------------------------------------------- completion

    @Test
    fun `finding every grid word completes the level and unlocks the next`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        vm.trace("CAT")
        vm.trace("ACT")
        vm.trace("AT")
        advanceUntilIdle()

        assertTrue(vm.state.value.isLevelComplete)
        assertTrue(progressRepository.completed.contains(1))
        assertEquals(2, progressRepository.progressState.value.highestUnlockedLevel)
    }

    @Test
    fun `words landed in quick succession are all accepted`() = runTest(dispatcher) {
        // Regression guard: an earlier implementation blocked input until each
        // accepted word's animation coroutine finished, so a fast player's second
        // and third words were silently swallowed.
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()

        vm.trace("CAT")
        vm.trace("ACT")   // committed before CAT's effect coroutine has run
        advanceUntilIdle()

        assertEquals(setOf("CAT", "ACT"), vm.state.value.foundWords)
    }

    @Test
    fun `the final level is flagged so the UI can offer level select instead of next`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(GameIntent.LoadLevel(2))
            advanceUntilIdle()

            assertTrue(vm.state.value.isLastLevel)
        }

    // ------------------------------------------------------------------ actions

    @Test
    fun `shuffle keeps the same letters and clears any in-flight selection`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(GameIntent.LoadLevel(1))
            advanceUntilIdle()
            val before = vm.state.value.wheelTiles

            vm.onIntent(GameIntent.BeginSelection(before[0]))
            vm.onIntent(GameIntent.Shuffle)

            val after = vm.state.value.wheelTiles
            assertEquals(before.map { it.id }.toSet(), after.map { it.id }.toSet())
            assertTrue(vm.state.value.selection.isEmpty())
        }

    @Test
    fun `a hint reveals one unsolved cell and never re-reveals a visible one`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(GameIntent.LoadLevel(1))
            advanceUntilIdle()

            vm.onIntent(GameIntent.UseHint)
            val first = vm.state.value.revealedCells
            assertEquals(1, first.size)

            vm.onIntent(GameIntent.UseHint)
            val second = vm.state.value.revealedCells
            assertEquals(2, second.size)
            assertTrue(second.containsAll(first))
        }

    @Test
    fun `pausing blocks new selections until resumed`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        val tiles = vm.state.value.wheelTiles

        vm.onIntent(GameIntent.Pause)
        vm.onIntent(GameIntent.BeginSelection(tiles[0]))
        assertTrue(vm.state.value.selection.isEmpty())

        vm.onIntent(GameIntent.Resume)
        vm.onIntent(GameIntent.BeginSelection(tiles[0]))
        assertEquals("C", vm.state.value.currentWord)
    }

    @Test
    fun `restarting a level clears found words`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        vm.trace("CAT")
        advanceUntilIdle()

        vm.onIntent(GameIntent.RestartLevel)
        advanceUntilIdle()

        assertTrue(vm.state.value.foundWords.isEmpty())
        assertEquals(0, vm.state.value.hintsUsed)
    }

    // -------------------------------------------------------------- persistence

    @Test
    fun `found words survive process death via SavedStateHandle`() = runTest(dispatcher) {
        val saved = SavedStateHandle()

        val first = viewModel(saved)
        first.onIntent(GameIntent.LoadLevel(1))
        advanceUntilIdle()
        first.trace("CAT")
        advanceUntilIdle()

        // A new ViewModel over the same SavedStateHandle stands in for the process
        // being killed and rebuilt.
        val restored = viewModel(saved)
        advanceUntilIdle()

        assertEquals(1, restored.state.value.level?.id)
        assertEquals(setOf("CAT"), restored.state.value.foundWords)
    }

    // ------------------------------------------------------------------ helpers

    /** Traces [word] on the wheel and lifts the finger. */
    private fun GameViewModel.trace(word: String) {
        val available = state.value.wheelTiles.toMutableList()
        word.forEachIndexed { index, ch ->
            val tile = available.first { it.char == ch }
            available.remove(tile)
            if (index == 0) onIntent(GameIntent.BeginSelection(tile))
            else onIntent(GameIntent.ExtendSelection(tile))
        }
        onIntent(GameIntent.CommitSelection)
    }
}

// ------------------------------------------------------------------------ fakes

private class FakeLevelRepository : LevelRepository {

    private val levels = listOf(
        Level(
            id = 1,
            letterTiles = listOf(LetterTile(0, 'C'), LetterTile(1, 'A'), LetterTile(2, 'T')),
            words = listOf("CAT", "ACT", "AT"),
            bonusWords = setOf("TA"),
            rows = 3,
            cols = 3,
            placements = listOf(
                Placement("CAT", 0, 0, Direction.VERTICAL),
                Placement("ACT", 2, 0, Direction.HORIZONTAL),
                Placement("AT", 1, 0, Direction.HORIZONTAL),
            ),
        ),
        Level(
            id = 2,
            letterTiles = listOf(LetterTile(0, 'D'), LetterTile(1, 'O'), LetterTile(2, 'G')),
            words = listOf("DOG"),
            bonusWords = setOf("GO"),
            rows = 1,
            cols = 3,
            placements = listOf(Placement("DOG", 0, 0, Direction.HORIZONTAL)),
        ),
    )

    override suspend fun getLevels(): List<Level> = levels
    override suspend fun getLevel(id: Int): Level? = levels.firstOrNull { it.id == id }
    override suspend fun levelCount(): Int = levels.size
}

private class FakeProgressRepository : ProgressRepository {

    val progressState = MutableStateFlow(PlayerProgress())
    val completed = mutableSetOf<Int>()

    override val progress: Flow<PlayerProgress> = progressState

    override suspend fun markLevelCompleted(levelId: Int, nextLevelId: Int?) {
        completed += levelId
        progressState.value = progressState.value.copy(
            completedLevels = completed.toSet(),
            highestUnlockedLevel = maxOf(
                progressState.value.highestUnlockedLevel,
                nextLevelId ?: progressState.value.highestUnlockedLevel,
            ),
        )
    }

    override suspend fun addBonusWords(count: Int) {
        progressState.value = progressState.value.copy(
            totalBonusWords = progressState.value.totalBonusWords + count,
        )
    }

    override suspend fun resetProgress() {
        completed.clear()
        progressState.value = PlayerProgress()
    }
}
