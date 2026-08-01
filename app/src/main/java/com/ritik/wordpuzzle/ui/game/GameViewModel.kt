package com.ritik.wordpuzzle.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.ritik.wordpuzzle.data.local.ProgressRepository
import com.ritik.wordpuzzle.data.repository.LevelRepository
import com.ritik.wordpuzzle.domain.model.GridPosition
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.domain.model.LetterTile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gameplay state machine.
 *
 * All mutation funnels through [onIntent], so there is exactly one way state can
 * change and every transition is traceable. State that must survive **process
 * death** (found words, hint reveals, current level) is mirrored into
 * [SavedStateHandle]; state that is purely visual (the in-flight finger selection)
 * deliberately is not, because a half-drawn swipe is meaningless after the process
 * is rebuilt.
 *
 * Rotation is handled by the ViewModel simply outliving the Activity, so no work is
 * needed there beyond keeping state here rather than in composables.
 */
class GameViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    // Channel (not SharedFlow) so effects buffer while the UI is backgrounded and
    // are delivered exactly once when it returns.
    private val _effects = Channel<GameEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Latched once the level-completing word lands, so the deferred celebration
     * cannot be triggered twice by a word committed during the reveal delay.
     */
    private var isCompletionPending = false

    init {
        // Restore after process death, if we have something to restore.
        savedStateHandle.get<Int>(KEY_LEVEL_ID)?.let { loadLevel(it, restoring = true) }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.LoadLevel -> {
                // Ignore a redundant reload — e.g. the composable re-running after
                // rotation — so found words are not wiped.
                if (_state.value.level?.id != intent.levelId) loadLevel(intent.levelId)
            }
            is GameIntent.BeginSelection -> beginSelection(intent.tile)
            is GameIntent.ExtendSelection -> extendSelection(intent.tile)
            GameIntent.CommitSelection -> commitSelection()
            GameIntent.CancelSelection -> _state.update { it.copy(selection = emptyList()) }
            GameIntent.Shuffle -> shuffle()
            GameIntent.UseHint -> useHint()
            GameIntent.Pause -> _state.update { it.copy(isPaused = true, selection = emptyList()) }
            GameIntent.Resume -> _state.update { it.copy(isPaused = false) }
            GameIntent.RestartLevel -> _state.value.level?.let { loadLevel(it.id, force = true) }
            GameIntent.AdvanceToNextLevel -> advanceToNextLevel()
            GameIntent.ConsumeFeedback -> _state.update { it.copy(feedback = null, lastAcceptedWord = null) }
        }
    }

    // ------------------------------------------------------------------ loading

    private fun loadLevel(levelId: Int, restoring: Boolean = false, force: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val level = levelRepository.getLevel(levelId)
            if (level == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            // On a restore, recover found words and hint reveals from saved state.
            val found = if (restoring && !force) savedStateHandle.restoredWords(KEY_FOUND) else emptySet()
            val bonus = if (restoring && !force) savedStateHandle.restoredWords(KEY_BONUS) else emptySet()
            val reveals = if (restoring && !force) savedStateHandle.restoredCells() else emptySet()
            val hints = if (restoring && !force) savedStateHandle.get<Int>(KEY_HINTS) ?: 0 else 0

            savedStateHandle[KEY_LEVEL_ID] = levelId
            if (force) persist(emptySet(), emptySet(), emptySet(), 0)

            isCompletionPending = false
            _state.update {
                GameUiState(
                    isLoading = false,
                    level = level,
                    foundWords = found,
                    foundBonusWords = bonus,
                    revealedCells = reveals,
                    wheelTiles = level.letterTiles,
                    hintsUsed = hints,
                    isLastLevel = levelId >= levelRepository.levelCount(),
                    isLevelComplete = found.size == level.words.size && level.words.isNotEmpty(),
                )
            }
        }
    }

    // ---------------------------------------------------------------- selection

    /**
     * Whether the wheel should respond to touches.
     *
     * Note this does *not* include a "a word is being processed" condition. Word
     * acceptance is synchronous, so a player swiping quickly can land several words
     * back to back; blocking input until each one's animation finished would
     * silently drop them.
     */
    private fun acceptsInput(): Boolean {
        val current = _state.value
        return !current.isPaused && !current.isLevelComplete && !isCompletionPending
    }

    private fun beginSelection(tile: LetterTile) {
        if (!acceptsInput()) return
        _state.update { it.copy(selection = listOf(tile), feedback = null) }
    }

    private fun extendSelection(tile: LetterTile) {
        if (!acceptsInput()) return
        _state.update { current ->
            val selection = current.selection
            when {
                selection.isEmpty() -> current.copy(selection = listOf(tile))

                // Still on the tile the trail already ends at: nothing to do. This
                // fires constantly, because a finger resting inside a tile keeps
                // producing move events.
                selection.last().id == tile.id -> current

                // Re-entering the second-to-last tile walks the trail back — the
                // standard affordance for correcting a swipe without lifting.
                // Guarded to >= 2 so a single-tile selection has no backtrack target.
                selection.size >= 2 && selection[selection.lastIndex - 1].id == tile.id ->
                    current.copy(selection = selection.dropLast(1))

                // Any other already-used tile is ignored: a letter cannot be
                // consumed twice in one word.
                selection.any { it.id == tile.id } -> current

                else -> current.copy(selection = selection + tile)
            }
        }
    }

    private fun commitSelection() {
        val current = _state.value
        if (current.selection.isEmpty() || isCompletionPending) return
        val level = current.level ?: return
        val word = current.currentWord

        when {
            word.length < MIN_WORD_LENGTH -> reject()

            word in current.foundWords || word in current.foundBonusWords ->
                finishSelection(WordFeedback.ALREADY_FOUND)

            word in level.words -> acceptGridWord(word, level)

            word in level.bonusWords -> acceptBonusWord(word)

            else -> reject()
        }
    }

    private fun acceptGridWord(word: String, level: Level) {
        val found = _state.value.foundWords + word
        _state.update {
            it.copy(
                foundWords = found,
                selection = emptyList(),
                feedback = WordFeedback.VALID,
                lastAcceptedWord = word,
            )
        }
        persist(found, _state.value.foundBonusWords, _state.value.revealedCells, _state.value.hintsUsed)

        val completesLevel = found.size == level.words.size
        // Latch completion synchronously. The celebration is deferred so the last
        // word's reveal animation can play, and without a latch a second word
        // committed during that window would run the completion path twice.
        if (completesLevel) {
            if (isCompletionPending) return
            isCompletionPending = true
        }

        viewModelScope.launch {
            _effects.send(GameEffect.WordAccepted(word))
            if (completesLevel) {
                delay(LEVEL_COMPLETE_DELAY_MS)
                progressRepository.markLevelCompleted(
                    levelId = level.id,
                    nextLevelId = (level.id + 1).takeIf { it <= levelRepository.levelCount() },
                )
                progressRepository.addBonusWords(_state.value.foundBonusWords.size)
                _state.update { it.copy(isLevelComplete = true) }
                _effects.send(GameEffect.LevelCompleted)
            }
        }
    }

    private fun acceptBonusWord(word: String) {
        val bonus = _state.value.foundBonusWords + word
        _state.update {
            it.copy(foundBonusWords = bonus, selection = emptyList(), feedback = WordFeedback.BONUS)
        }
        persist(_state.value.foundWords, bonus, _state.value.revealedCells, _state.value.hintsUsed)
        viewModelScope.launch { _effects.send(GameEffect.BonusWordAccepted(word)) }
    }

    private fun reject() {
        finishSelection(WordFeedback.INVALID)
        viewModelScope.launch { _effects.send(GameEffect.WordRejected) }
    }

    private fun finishSelection(feedback: WordFeedback) {
        _state.update { it.copy(selection = emptyList(), feedback = feedback) }
    }

    // ------------------------------------------------------------------ actions

    private fun shuffle() {
        _state.update { it.copy(wheelTiles = it.wheelTiles.shuffled(), selection = emptyList()) }
    }

    /**
     * Reveals one letter of an unsolved word: the first cell of that word which is
     * not already visible. Cheap, deterministic, and never wasted on a revealed cell.
     */
    private fun useHint() {
        val current = _state.value
        val level = current.level ?: return
        if (current.isLevelComplete) return

        val visible = visibleCells(current)
        val target = level.placements
            .filter { it.word !in current.foundWords }
            .flatMap { it.cells }
            .firstOrNull { it !in visible }
            ?: return

        val reveals = current.revealedCells + target
        val hints = current.hintsUsed + 1
        _state.update { it.copy(revealedCells = reveals, hintsUsed = hints) }
        persist(current.foundWords, current.foundBonusWords, reveals, hints)
    }

    private fun advanceToNextLevel() {
        val level = _state.value.level ?: return
        viewModelScope.launch {
            val next = level.id + 1
            if (next <= levelRepository.levelCount()) {
                _effects.send(GameEffect.NavigateToLevel(next))
            } else {
                _effects.send(GameEffect.NavigateHome)
            }
        }
    }

    // ---------------------------------------------------------------- persistence

    private fun persist(
        found: Set<String>,
        bonus: Set<String>,
        reveals: Set<GridPosition>,
        hints: Int,
    ) {
        savedStateHandle[KEY_FOUND] = ArrayList(found)
        savedStateHandle[KEY_BONUS] = ArrayList(bonus)
        savedStateHandle[KEY_REVEALS] = ArrayList(reveals.map { "${it.row},${it.col}" })
        savedStateHandle[KEY_HINTS] = hints
    }

    private fun SavedStateHandle.restoredWords(key: String): Set<String> =
        get<ArrayList<String>>(key)?.toSet().orEmpty()

    private fun SavedStateHandle.restoredCells(): Set<GridPosition> =
        get<ArrayList<String>>(KEY_REVEALS)
            ?.mapNotNull { raw ->
                val parts = raw.split(',')
                val r = parts.getOrNull(0)?.toIntOrNull()
                val c = parts.getOrNull(1)?.toIntOrNull()
                if (r != null && c != null) GridPosition(r, c) else null
            }
            ?.toSet()
            .orEmpty()

    companion object {
        private const val MIN_WORD_LENGTH = 2
        private const val LEVEL_COMPLETE_DELAY_MS = 620L

        private const val KEY_LEVEL_ID = "level_id"
        private const val KEY_FOUND = "found_words"
        private const val KEY_BONUS = "bonus_words"
        private const val KEY_REVEALS = "revealed_cells"
        private const val KEY_HINTS = "hints_used"

        /** Cells visible either because their word is found or a hint revealed them. */
        fun visibleCells(state: GameUiState): Set<GridPosition> {
            val level = state.level ?: return state.revealedCells
            val fromWords = level.placements
                .filter { it.word in state.foundWords }
                .flatMap { it.cells }
            return fromWords.toSet() + state.revealedCells
        }

        fun factory(
            levelRepository: LevelRepository,
            progressRepository: ProgressRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(
                    levelRepository = levelRepository,
                    progressRepository = progressRepository,
                    savedStateHandle = extras.createSavedStateHandle(),
                ) as T
            }
        }
    }
}
