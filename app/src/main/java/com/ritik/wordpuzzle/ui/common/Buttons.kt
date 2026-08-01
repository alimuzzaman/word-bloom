package com.ritik.wordpuzzle.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentAmberDeep
import com.ritik.wordpuzzle.ui.theme.InkOnLight
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

/**
 * Shared button styles.
 *
 * Both variants press-scale via a spring rather than relying on ripple alone: on a
 * dark, playful surface the squash reads as far more responsive than a ripple, and
 * it keeps touch feedback consistent across every screen.
 */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "primaryPress",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 56.dp)
            .background(
                brush = if (enabled) {
                    Brush.verticalGradient(listOf(AccentAmber, AccentAmberDeep))
                } else {
                    Brush.verticalGradient(listOf(Color.Gray, Color.DarkGray))
                },
                shape = RoundedCornerShape(18.dp),
            )
            .clickableScaled(interactionSource, enabled, onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            color = InkOnLight,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = WordPuzzleTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "secondaryPress",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 56.dp)
            .background(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp))
            .border(
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(18.dp),
            )
            .clickableScaled(interactionSource, enabled, onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

/** Shared clickable wiring so both variants get identical interaction semantics. */
@Composable
private fun Modifier.clickableScaled(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick,
)
