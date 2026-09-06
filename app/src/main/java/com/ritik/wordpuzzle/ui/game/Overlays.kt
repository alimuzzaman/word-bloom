package com.ritik.wordpuzzle.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.ritik.wordpuzzle.R
import com.ritik.wordpuzzle.ui.common.PrimaryButton
import com.ritik.wordpuzzle.ui.common.SecondaryButton
import com.ritik.wordpuzzle.ui.learning.LearningWordCardData
import com.ritik.wordpuzzle.ui.learning.LearningStoryModal
import com.ritik.wordpuzzle.ui.learning.LevelLearningContent
import com.ritik.wordpuzzle.ui.learning.LessonCardData
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

/**
 * Pause menu.
 *
 * A full-screen composable rather than a `Dialog` so it animates with the same
 * spring vocabulary as the rest of the game and cannot be dismissed accidentally by
 * a tap outside — a mis-tap that quit a level would be infuriating.
 */
@Composable
fun PauseOverlay(
    visible: Boolean,
    levelId: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onLevelSelect: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrimOverlay(visible = visible, modifier = modifier) {
        OverlayCard {
            Text(
                text = stringResource(R.string.pause_title),
                color = WordPuzzleTheme.colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.game_level_label, levelId),
                color = WordPuzzleTheme.colors.textSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(26.dp))

            PrimaryButton(
                text = stringResource(R.string.pause_resume),
                onClick = onResume,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.pause_restart),
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.pause_levels),
                onClick = onLevelSelect,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.pause_home),
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Level-complete celebration.
 *
 * The completion content now lives in the shared learning modal. The small
 * rotating sweep remains in the modal body so the celebration survives the
 * single-scroll layout.
 */
@Composable
fun LevelCompleteOverlay(
    visible: Boolean,
    levelId: Int,
    bonusWordCount: Int,
    isLastLevel: Boolean,
    lesson: LessonCardData? = null,
    /** All target meanings, shown after the lesson because completion is safe for answers. */
    words: List<LearningWordCardData> = emptyList(),
    onNext: () -> Unit,
    onLevelSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LearningStoryModal(
        visible = visible,
        title = stringResource(R.string.complete_title),
        subtitle = if (bonusWordCount > 0) {
            "Level $levelId · $bonusWordCount bonus word${if (bonusWordCount == 1) "" else "s"}"
        } else {
            "Level $levelId"
        },
        contentKey = "completion-$levelId",
        // A completion screen must be resolved with one of its actions. Back and
        // outside taps are intentionally not dismissal paths here.
        onDismiss = {},
        showClose = false,
        modifier = modifier,
        footer = {
            if (isLastLevel) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.complete_all_done),
                        color = AccentTeal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.pause_levels),
                        onClick = onLevelSelect,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.complete_next),
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.pause_levels),
                        onClick = onLevelSelect,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CelebrationBurst(modifier = Modifier.size(150.dp))
            Text(text = "★", color = AccentAmber, fontSize = 46.sp)
        }
        LevelLearningContent(
            lesson = lesson,
            words = words,
            glossaryTitle = "All target words · সব লক্ষ্যশব্দ",
            emptyText = "This level has no word meanings yet.\nএই স্তরে এখনও কোনো শব্দের অর্থ নেই।",
            showDetailedWords = true,
        )
    }
}

/** Shared scrim + entry/exit animation for both overlays. */
@Composable
private fun ScrimOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                // Swallow taps so the board beneath cannot be interacted with.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(32.dp),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ) + fadeIn(tween(180)),
                exit = scaleOut(targetScale = 0.9f) + fadeOut(tween(140)),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun OverlayCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        WordPuzzleTheme.colors.surfaceElevated,
                        WordPuzzleTheme.colors.surface,
                    ),
                ),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 26.dp, vertical = 32.dp),
        content = content,
    )
}

@Composable
private fun CelebrationBurst(modifier: Modifier = Modifier.size(420.dp)) {
    val infinite = rememberInfiniteTransition(label = "burst")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "burstRotation",
    )

    Box(
        modifier = modifier
            .rotate(rotation)
            .background(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        AccentAmber.copy(alpha = 0.16f),
                        Color.Transparent,
                        AccentTeal.copy(alpha = 0.13f),
                        Color.Transparent,
                        AccentAmber.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
