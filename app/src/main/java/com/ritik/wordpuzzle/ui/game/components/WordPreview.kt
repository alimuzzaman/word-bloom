package com.ritik.wordpuzzle.ui.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritik.wordpuzzle.ui.game.WordFeedback
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

/**
 * Floating preview of the word being traced.
 *
 * Also carries validation feedback: on an invalid word the chip shakes horizontally,
 * which communicates rejection faster than any text and is the convention players
 * already know from this genre.
 */
@Composable
fun WordPreview(
    word: String,
    feedback: WordFeedback?,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors

    // The chip shows either the in-flight word or the just-rejected/accepted one.
    val visible = word.isNotEmpty() || feedback != null

    val chipColor by animateColorAsState(
        targetValue = when (feedback) {
            WordFeedback.VALID -> colors.feedbackValid
            WordFeedback.BONUS -> colors.feedbackBonus
            WordFeedback.INVALID -> colors.feedbackInvalid
            WordFeedback.ALREADY_FOUND -> colors.feedbackRepeat
            null -> colors.trail
        },
        animationSpec = tween(140),
        label = "chipColor",
    )

    // Shake offset, driven imperatively so each rejection restarts the animation.
    val shake = remember { Animatable(0f, Float.VectorConverter) }
    LaunchedEffect(feedback, word) {
        if (feedback == WordFeedback.INVALID || feedback == WordFeedback.ALREADY_FOUND) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    0f at 0
                    -14f at 60
                    12f at 120
                    -8f at 180
                    5f at 240
                    0f at 320
                },
            )
        } else {
            shake.snapTo(0f)
        }
    }

    Box(modifier = modifier.height(54.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.86f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
            )),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.9f),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer { translationX = shake.value }
                    .background(color = chipColor, shape = RoundedCornerShape(percent = 50))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    text = displayText(word, feedback),
                    color = colors.tileText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 3.sp,
                )
            }
        }
    }
}

private fun displayText(word: String, feedback: WordFeedback?): String = when {
    word.isNotEmpty() -> word
    feedback == WordFeedback.BONUS -> "BONUS!"
    feedback == WordFeedback.ALREADY_FOUND -> "FOUND"
    else -> ""
}
