package com.ritik.wordpuzzle.ui.splash

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import kotlinx.coroutines.delay

/**
 * Branded splash.
 *
 * Deliberately short ([SPLASH_DURATION_MS]) — long enough for the logo animation to
 * land, short enough not to tax the player. [onFinished] is wrapped in
 * [rememberUpdatedState] so the timer coroutine is never restarted by a
 * recomposition that hands in a fresh lambda.
 */
@Composable
fun SplashRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val currentOnFinished by rememberUpdatedState(onFinished)

    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(320))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
        titleAlpha.animateTo(1f, tween(280))
        delay(SPLASH_DURATION_MS)
        currentOnFinished()
    }

    // Slow rotation on the halo gives the static logo a sense of life.
    val infinite = rememberInfiniteTransition(label = "splashHalo")
    val haloRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "haloRotation",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Rotating halo behind the mark.
                Box(
                    Modifier
                        .size(168.dp)
                        .rotate(haloRotation)
                        .graphicsLayer { alpha = logoAlpha.value * 0.5f }
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    AccentAmber.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    AccentAmber.copy(alpha = 0.35f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
                )

                Text(
                    text = "W",
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentAmber,
                    modifier = Modifier
                        .scale(logoScale.value)
                        .graphicsLayer { alpha = logoAlpha.value },
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "WORD BLOOM",
                color = colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Swipe letters. Bloom words.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )
        }
    }
}

private const val SPLASH_DURATION_MS = 900L
