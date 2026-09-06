package com.ritik.wordpuzzle.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritik.wordpuzzle.R
import com.ritik.wordpuzzle.di.AppContainer
import com.ritik.wordpuzzle.ui.game.components.CrosswordGrid
import com.ritik.wordpuzzle.ui.game.components.LetterWheel
import com.ritik.wordpuzzle.ui.game.components.WordPreview
import com.ritik.wordpuzzle.ui.levelselect.IconCircleButton
import com.ritik.wordpuzzle.ui.learning.FoundWordsOverlay
import com.ritik.wordpuzzle.ui.learning.LearningWordCardData
import com.ritik.wordpuzzle.ui.learning.LessonCardData
import com.ritik.wordpuzzle.ui.learning.addLessonExamples
import com.ritik.wordpuzzle.ui.learning.dedupeLearningWords
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import com.ritik.wordpuzzle.util.Haptics

/**
 * Gameplay screen.
 *
 * Owns no game state of its own: everything comes from [GameViewModel] and every
 * interaction leaves as a [GameIntent]. That is what makes rotation free — the
 * composable can be destroyed and rebuilt and the board is unchanged.
 */
@Composable
fun GameRoute(
    categoryId: String,
    levelId: Int,
    appContainer: AppContainer,
    onNavigateToLevel: (Int) -> Unit,
    onNavigateToLevelSelect: () -> Unit,
    onNavigateHome: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GameViewModel = viewModel(
        factory = GameViewModel.factory(
            appContainer.levelRepository,
            appContainer.progressRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val haptics = remember(view) { Haptics(view) }
    var showFoundWords by rememberSaveable { mutableStateOf(false) }

    // Load once per level id. Guarded inside the ViewModel too, so a recomposition
    // or rotation cannot reset the board.
    LaunchedEffect(categoryId, levelId) {
        showFoundWords = false
        viewModel.onIntent(GameIntent.LoadLevel(categoryId, levelId))
    }

    // Auto-pause when the app goes to the background, so a player returning from a
    // phone call is not staring at a live board they have lost their place in.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (!state.isLevelComplete) viewModel.onIntent(GameIntent.Pause)
    }

    // One-shot effects: feedback and navigation.
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GameEffect.WordAccepted -> haptics.success()
                is GameEffect.BonusWordAccepted -> haptics.light()
                GameEffect.WordRejected -> haptics.reject()
                GameEffect.LevelCompleted -> haptics.celebrate()
                is GameEffect.NavigateToLevel -> onNavigateToLevel(effect.levelId)
                is GameEffect.NavigateToLevelSelect -> onNavigateToLevelSelect()
                GameEffect.NavigateHome -> onNavigateHome()
            }
        }
    }

    // System back: pause first, then let a second back leave the level. This is the
    // behaviour players expect from a game — back should never dump you out of a
    // level you are mid-way through without warning.
    BackHandler(enabled = showFoundWords) {
        showFoundWords = false
    }
    BackHandler(enabled = !state.isPaused && !state.isLevelComplete) {
        viewModel.onIntent(GameIntent.Pause)
    }

    GameScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onLearn = { showFoundWords = true },
        onQuitToHome = onNavigateHome,
        onGoToLevelSelect = onNavigateToLevelSelect,
        modifier = modifier,
    )

    val level = state.level
    if (level != null) {
        val allWordCards = level.toLearningWordCards()
        FoundWordsOverlay(
            // The completion modal owns the full glossary. Do not leave the
            // active-level book sheet above it when the final word is found.
            visible = showFoundWords && !state.isLevelComplete,
            levelLabel = "Level ${level.id} · ${state.foundWords.size}/${state.totalWords} words",
            foundWords = allWordCards.filter { card ->
                state.foundWords.any { it.equals(card.word, ignoreCase = true) }
            },
            lesson = level.toLessonCard(),
            allWords = allWordCards,
            // Unfinished levels reveal only words the player has already found.
            // Completion shows the full, detailed glossary below.
            showAllWords = false,
            onDismiss = { showFoundWords = false },
        )
    }
}

private fun Level.toLearningWordCards(): List<LearningWordCardData> {
    val meaningsByWord = wordMeanings.associateBy { meaning -> meaning.word.uppercase() }
    val cards = dedupeLearningWords(words.mapNotNull { rawWord ->
        val word = rawWord.uppercase()
        meaningsByWord[word]?.let { meaning ->
            LearningWordCardData(
                word = meaning.word,
                definitionEn = meaning.definitionEn,
                translationBn = meaning.translationBn,
                meaningBn = meaning.meaningBn,
            )
        } ?: if (heroWord.word.equals(word, ignoreCase = true)) {
            LearningWordCardData(
                word = heroWord.word,
                definitionEn = heroWord.definitionEn,
                translationBn = heroWord.translationBn,
                meaningBn = heroWord.meaningBn,
            )
        } else {
            null
        }
    })
    return addLessonExamples(cards, toLessonCard())
}

private fun Level.toLessonCard(): LessonCardData = LessonCardData(
    heroWord = LearningWordCardData(
        word = heroWord.word,
        definitionEn = heroWord.definitionEn,
        translationBn = heroWord.translationBn,
        meaningBn = heroWord.meaningBn,
    ),
    sentenceEn = lesson?.sentenceEn.orEmpty(),
    sentenceBn = lesson?.sentenceBn.orEmpty(),
    usedWords = lesson?.usedWords.orEmpty(),
)

@Composable
private fun GameScreen(
    state: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onLearn: () -> Unit,
    onQuitToHome: () -> Unit,
    onGoToLevelSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom))),
    ) {
        when {
            state.isLoading || state.level == null -> {
                CircularProgressIndicator(
                    color = AccentAmber,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                GameContent(state = state, onIntent = onIntent, onLearn = onLearn)
            }
        }

        // Pause overlay — state, not a destination. See WordPuzzleNavHost for why.
        PauseOverlay(
            visible = state.isPaused && !state.isLevelComplete,
            levelId = state.level?.id ?: 0,
            onResume = { onIntent(GameIntent.Resume) },
            onRestart = { onIntent(GameIntent.RestartLevel) },
            onLevelSelect = onGoToLevelSelect,
            onQuit = onQuitToHome,
        )

        LevelCompleteOverlay(
            visible = state.isLevelComplete,
            levelId = state.level?.id ?: 0,
            bonusWordCount = state.foundBonusWords.size,
            isLastLevel = state.isLastLevel,
            lesson = state.level?.let { level ->
                level.toLessonCard()
            },
            words = state.level?.toLearningWordCards().orEmpty(),
            onNext = { onIntent(GameIntent.AdvanceToNextLevel) },
            onLevelSelect = onGoToLevelSelect,
        )
    }
}

@Composable
private fun GameContent(
    state: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onLearn: () -> Unit,
) {
    val level = state.level ?: return
    val colors = WordPuzzleTheme.colors

    // Dim and disable the board while paused so the overlay clearly owns the screen.
    val boardAlpha by animateFloatAsState(
        targetValue = if (state.isPaused) 0.25f else 1f,
        animationSpec = tween(220),
        label = "boardAlpha",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
    // Size the wheel from whichever axis is tighter. Deriving it from width alone
    // overflows the bottom of short or split-screen windows.
    val wheelSize = minOf(
        maxWidth * WHEEL_WIDTH_FRACTION,
        maxHeight * WHEEL_HEIGHT_FRACTION,
    ).coerceIn(WHEEL_MIN, WHEEL_MAX)

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            levelId = level.id,
            foundCount = state.foundWords.size,
            totalCount = state.totalWords,
            bonusCount = state.foundBonusWords.size,
            progress = state.progressFraction,
            onLearn = onLearn,
            onPause = { onIntent(GameIntent.Pause) },
        )

        // Board takes the space the wheel does not need, and the grid centres
        // itself inside it. Small levels (a 3x3 grid) therefore sit in the middle
        // of that area rather than hugging the top bar.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .alpha(boardAlpha),
        ) {
            CrosswordGrid(
                level = level,
                foundWords = state.foundWords,
                revealedCells = state.revealedCells,
                lastAcceptedWord = state.lastAcceptedWord,
                modifier = Modifier.fillMaxSize(),
            )
        }

        WordPreview(
            word = state.currentWord,
            feedback = state.feedback,
            modifier = Modifier.fillMaxWidth(),
        )

        // Clear transient feedback shortly after it appears, so the chip does not
        // linger once its animation has served its purpose.
        LaunchedEffect(state.feedback, state.lastAcceptedWord) {
            if (state.feedback != null && state.currentWord.isEmpty()) {
                kotlinx.coroutines.delay(FEEDBACK_LINGER_MS)
                onIntent(GameIntent.ConsumeFeedback)
            }
        }

        Spacer(Modifier.height(4.dp))

        // The wheel keeps a fixed diameter rather than filling the row, so the
        // action buttons sit just outside it instead of at the screen bezels.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            LetterWheel(
                tiles = state.wheelTiles,
                selection = state.selection,
                onSelectionStart = { onIntent(GameIntent.BeginSelection(it)) },
                onSelectionExtend = { onIntent(GameIntent.ExtendSelection(it)) },
                onSelectionCommit = { onIntent(GameIntent.CommitSelection) },
                onSelectionCancel = { onIntent(GameIntent.CancelSelection) },
                enabled = !state.isPaused && !state.isLevelComplete,
                modifier = Modifier.size(wheelSize),
            )

            IconCircleButton(
                icon = Icons.Default.Lightbulb,
                contentDescription = stringResource(R.string.cd_hint),
                onClick = { onIntent(GameIntent.UseHint) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),
            )
            IconCircleButton(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.cd_shuffle),
                onClick = { onIntent(GameIntent.Shuffle) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp),
            )
        }
    }
    }
}

@Composable
private fun GameTopBar(
    levelId: Int,
    foundCount: Int,
    totalCount: Int,
    bonusCount: Int,
    progress: Float,
    onLearn: () -> Unit,
    onPause: () -> Unit,
) {
    val colors = WordPuzzleTheme.colors
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(420),
        label = "levelProgress",
    )

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(
                icon = Icons.Default.Pause,
                contentDescription = stringResource(R.string.cd_pause),
                onClick = onPause,
            )
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.game_level_label, levelId),
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "$foundCount of $totalCount words",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            IconCircleButton(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = stringResource(R.string.cd_learn_words),
                onClick = onLearn,
            )
            Spacer(Modifier.size(8.dp))
            if (bonusCount > 0) {
                Box(
                    modifier = Modifier
                        .background(AccentAmber.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "★ $bonusCount",
                        color = AccentAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Progress bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(listOf(AccentTeal, AccentAmber)),
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}

private const val FEEDBACK_LINGER_MS = 700L

/** Wheel diameter as a fraction of width — leaves room for the side buttons. */
private const val WHEEL_WIDTH_FRACTION = 0.66f

/** …and of height, so the wheel cannot overflow a short window. */
private const val WHEEL_HEIGHT_FRACTION = 0.34f

private val WHEEL_MIN = 200.dp
private val WHEEL_MAX = 300.dp
