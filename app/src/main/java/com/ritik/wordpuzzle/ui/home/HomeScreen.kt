package com.ritik.wordpuzzle.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.ritik.wordpuzzle.di.AppContainer
import com.ritik.wordpuzzle.ui.common.PrimaryButton
import com.ritik.wordpuzzle.ui.common.SecondaryButton
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import kotlin.math.sin

/**
 * Home screen.
 *
 * Reads progress from DataStore so the primary action is "Continue level N" for a
 * returning player and "Play" for a new one — the player never has to hunt for
 * where they left off.
 */
@Composable
fun HomeRoute(
    appContainer: AppContainer,
    onPlay: (Int) -> Unit,
    onLevelSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(appContainer.levelRepository, appContainer.progressRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onPlay = { onPlay(state.continueLevelId) },
        onLevelSelect = onLevelSelect,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onPlay: () -> Unit,
    onLevelSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors

    // Staggered entry: title, then subtitle, then the action block.
    //
    // The three run concurrently with offset start delays rather than as sequential
    // awaits. Chaining them would gate the buttons behind the full duration of the
    // earlier animations, leaving them invisible — and untappable — for most of a
    // second after the screen appears.
    val titleAlpha = remember { Animatable(0f) }
    val bodyAlpha = remember { Animatable(0f) }
    val actionsAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { titleAlpha.animateTo(1f, tween(durationMillis = 420)) }
        launch { bodyAlpha.animateTo(1f, tween(durationMillis = 380, delayMillis = 160)) }
        launch {
            actionsAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, delayMillis = 280),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom))),
    ) {
        FloatingLetters()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
        ) {
            Text(
                text = "WORD",
                color = colors.textPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )
            Text(
                text = "BLOOM",
                color = AccentAmber,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Swipe letters. Bloom words.",
                color = colors.textSecondary,
                fontSize = 15.sp,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = bodyAlpha.value },
            )

            Spacer(Modifier.height(48.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = actionsAlpha.value
                        translationY = (1f - actionsAlpha.value) * 40f
                    },
            ) {
                PrimaryButton(
                    text = if (state.hasProgress) {
                        "Continue · Level ${state.continueLevelId}"
                    } else {
                        "Play"
                    },
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))

                SecondaryButton(
                    text = "Levels",
                    onClick = onLevelSelect,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.hasProgress) {
                Spacer(Modifier.height(36.dp))
                StatRow(
                    completed = state.completedCount,
                    total = state.totalLevels,
                    bonusWords = state.totalBonusWords,
                    modifier = Modifier.graphicsLayer { alpha = actionsAlpha.value },
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    completed: Int,
    total: Int,
    bonusWords: Int,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        modifier = modifier,
    ) {
        StatCell(value = "$completed/$total", label = "LEVELS", accent = AccentTeal)
        StatCell(value = "$bonusWords", label = "BONUS WORDS", accent = AccentAmber)
    }
}

@Composable
private fun StatCell(value: String, label: String, accent: Color) {
    val colors = WordPuzzleTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = accent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
    }
}

/**
 * Ambient background motion: a few translucent letters drifting on sine paths.
 * Purely decorative, and cheap — six composables driven by one infinite transition.
 */
@Composable
private fun FloatingLetters(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "floatingLetters")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "phase",
    )

    val configuration = LocalConfiguration.current
    val widthPx = configuration.screenWidthDp.toFloat()
    val heightPx = configuration.screenHeightDp.toFloat()

    val letters = remember { listOf('W', 'O', 'R', 'D', 'S', 'B') }

    Box(modifier = modifier.fillMaxSize()) {
        letters.forEachIndexed { index, letter ->
            val seed = index * 1.7f
            val x = (widthPx * (0.1f + 0.16f * index)).dp
            val y = (heightPx * (0.12f + 0.14f * index) + sin(phase + seed) * 18f).dp
            Text(
                text = letter.toString(),
                color = Color.White.copy(alpha = 0.05f),
                fontSize = (34 + index * 6).sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .padding(start = x, top = y)
                    .graphicsLayer { rotationZ = sin(phase + seed) * 9f },
            )
        }
    }
}
