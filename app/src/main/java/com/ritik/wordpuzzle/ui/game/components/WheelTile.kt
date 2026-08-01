package com.ritik.wordpuzzle.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import kotlin.math.roundToInt

/**
 * One letter tile on the wheel.
 *
 * Positioned by a custom [layout] that places the tile's *centre* at [centre],
 * because the wheel maths works in centre coordinates — offsetting by top-left
 * would force every call site to subtract half the tile size.
 *
 * Selection is animated with a spring rather than a tween: the slight overshoot
 * reads as physical and gives the swipe its tactile quality.
 */
@Composable
fun WheelTile(
    letter: Char,
    isSelected: Boolean,
    centre: Offset,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val density = LocalDensity.current

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.16f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tileScale",
    )
    val faceColor by animateColorAsState(
        targetValue = if (isSelected) colors.tileSelected else colors.tileFace,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileFace",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.55f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tileGlow",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = (centre.x - placeable.width / 2f).roundToInt(),
                        y = (centre.y - placeable.height / 2f).roundToInt(),
                    )
                }
            }
            .size(size)
            .scale(scale)
            // Halo behind the selected tile; drawn rather than shadowed so it can
            // exceed the tile bounds without affecting layout.
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawCircle(
                        color = colors.tileSelected.copy(alpha = glowAlpha * 0.5f),
                        radius = this.size.minDimension / 2f * 1.34f,
                    )
                }
            }
            .background(color = faceColor, shape = CircleShape)
            .semantics { contentDescription = "Letter $letter" },
    ) {
        Text(
            text = letter.toString(),
            color = if (isSelected) colors.tileSelectedText else colors.tileText,
            fontSize = with(density) { (size.toPx() * 0.44f).toSp() },
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
