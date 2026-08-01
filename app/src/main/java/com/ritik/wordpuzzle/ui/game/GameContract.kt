package com.ritik.wordpuzzle.ui.game

import androidx.compose.runtime.Immutable
import com.ritik.wordpuzzle.domain.model.GridPosition
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.domain.model.LetterTile

/**
 * MVI contract for the gameplay screen.
 *
 * One immutable [GameUiState] describes everything on screen; the UI sends
 * [GameIntent]s and never mutates state directly; one-shot occurrences that must not
 * replay on recomposition or rotation (haptics, sounds, navigation) travel as
 * [GameEffect]s on a channel rather than living in state.
 */

@Immutable
data class GameUiState(
    val isLoading: Boolean = true,
    val level: Level? = null,
    /** Grid words the player has found. */
    val foundWords: Set<String> = emptySet(),
    /** Off-grid bonus words the player has found. */
    val foundBonusWords: Set<String> = emptySet(),
    /** Tiles currently traced by the finger, in selection order. */
    val selection: List<LetterTile> = emptyList(),
    /** Wheel order; changes on shuffle. */
    val wheelTiles: List<LetterTile> = emptyList(),
    /** Individual letters revealed by hints, so they render even before their word is found. */
    val revealedCells: Set<GridPosition> = emptySet(),
    /** Drives the transient feedback banner and word-preview colouring. */
    val feedback: WordFeedback? = null,
    /** Word that just landed on the grid — powers the per-cell reveal animation. */
    val lastAcceptedWord: String? = null,
    val isPaused: Boolean = false,
    val isLevelComplete: Boolean = false,
    val hintsUsed: Int = 0,
    val isLastLevel: Boolean = false,
) {
    /** Live preview of the traced word. */
    val currentWord: String get() = selection.map { it.char }.joinToString("")

    val totalWords: Int get() = level?.words?.size ?: 0

    val progressFraction: Float
        get() = if (totalWords == 0) 0f else foundWords.size.toFloat() / totalWords
}

/** Result of validating a traced word — drives colour and animation of the preview. */
enum class WordFeedback { VALID, BONUS, ALREADY_FOUND, INVALID }

/** Everything the gameplay UI can ask the ViewModel to do. */
sealed interface GameIntent {
    data class LoadLevel(val levelId: Int) : GameIntent

    /** Finger touched down on a wheel tile. */
    data class BeginSelection(val tile: LetterTile) : GameIntent

    /** Finger dragged onto another tile. */
    data class ExtendSelection(val tile: LetterTile) : GameIntent

    /** Finger lifted — the traced word gets validated. */
    data object CommitSelection : GameIntent

    /** Gesture cancelled (e.g. by an incoming call) without committing. */
    data object CancelSelection : GameIntent

    data object Shuffle : GameIntent
    data object UseHint : GameIntent
    data object Pause : GameIntent
    data object Resume : GameIntent
    data object RestartLevel : GameIntent
    data object AdvanceToNextLevel : GameIntent

    /** Clears a transient banner once its animation has run. */
    data object ConsumeFeedback : GameIntent
}

/** One-shot events. Delivered once and never replayed after rotation. */
sealed interface GameEffect {
    data class WordAccepted(val word: String) : GameEffect
    data class BonusWordAccepted(val word: String) : GameEffect
    data object WordRejected : GameEffect
    data object LevelCompleted : GameEffect
    data class NavigateToLevel(val levelId: Int) : GameEffect
    data object NavigateHome : GameEffect
}
