package com.ritik.wordpuzzle.ui.game.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.ritik.wordpuzzle.domain.model.GridPosition
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import kotlinx.coroutines.delay

/**
 * The crossword board.
 *
 * Cell size is derived from the available space and the level's dimensions, so a
 * 3×3 level and a 10×10 level both fill the board area without clipping or manual
 * per-level tuning.
 *
 * When a word is solved its letters do not simply appear: each cell in the word
 * flips in with a staggered delay ([REVEAL_STAGGER_MS] apart), so the eye follows
 * the word left-to-right as it lands. That stagger is the single most satisfying
 * micro-interaction in the game, and it is why [lastAcceptedWord] is threaded down
 * here rather than the grid just diffing its filled set.
 */
@Composable
fun CrosswordGrid(
    level: Level,
    foundWords: Set<String>,
    revealedCells: Set<GridPosition>,
    lastAcceptedWord: String?,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors

    // Cells whose letter should currently be visible.
    val visibleCells: Map<GridPosition, Char> = remember(level, foundWords, revealedCells) {
        buildMap {
            level.placements
                .filter { it.word in foundWords }
                .forEach { placement ->
                    placement.cells.zip(placement.word.toList()).forEach { (pos, ch) -> put(pos, ch) }
                }
            revealedCells.forEach { pos -> level.occupiedCells[pos]?.let { put(pos, it) } }
        }
    }

    // Per-cell animation delay for the word that just landed.
    val staggerDelays: Map<GridPosition, Int> = remember(lastAcceptedWord, level) {
        val placement = lastAcceptedWord?.let { word -> level.placements.firstOrNull { it.word == word } }
        placement?.cells
            ?.mapIndexed { index, pos -> pos to index * REVEAL_STAGGER_MS }
            ?.toMap()
            .orEmpty()
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val spacing = 4.dp

        // Fit the grid to whichever axis is tighter.
        val cellSize: Dp = remember(maxWidth, maxHeight, level.rows, level.cols) {
            val totalSpacingW = spacing * (level.cols - 1).coerceAtLeast(0)
            val totalSpacingH = spacing * (level.rows - 1).coerceAtLeast(0)
            val byWidth = (maxWidth - totalSpacingW) / level.cols.coerceAtLeast(1)
            val byHeight = (maxHeight - totalSpacingH) / level.rows.coerceAtLeast(1)
            minOf(byWidth, byHeight).coerceIn(MIN_CELL, MAX_CELL)
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            for (row in 0 until level.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (col in 0 until level.cols) {
                        val position = GridPosition(row, col)
                        val isPartOfWord = level.occupiedCells.containsKey(position)
                        if (!isPartOfWord) {
                            // Keep the layout rigid: empty cells still occupy space.
                            Box(Modifier.size(cellSize))
                        } else {
                            GridCell(
                                letter = visibleCells[position],
                                size = cellSize,
                                revealDelayMs = staggerDelays[position] ?: 0,
                                isHintRevealed = position in revealedCells &&
                                    level.placements.none { it.word in foundWords && position in it.cells },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single crossword square.
 *
 * The reveal is a Y-axis rotation from 90° to 0° combined with a scale pop, which
 * reads as the tile flipping face-up. [revealDelayMs] staggers it within its word.
 */
@Composable
private fun GridCell(
    letter: Char?,
    size: Dp,
    revealDelayMs: Int,
    isHintRevealed: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val density = LocalDensity.current
    val isFilled = letter != null

    // Gate the animation behind the stagger delay so cells in a word land in order.
    var revealed by remember(letter) { mutableStateOf(letter == null) }
    LaunchedEffect(letter, revealDelayMs) {
        if (letter != null && !revealed) {
            if (revealDelayMs > 0) delay(revealDelayMs.toLong())
            revealed = true
        }
    }

    val flip by animateFloatAsState(
        targetValue = if (revealed && isFilled) 0f else 90f,
        animationSpec = tween(durationMillis = REVEAL_DURATION_MS),
        label = "cellFlip",
    )
    val pop by animateFloatAsState(
        targetValue = if (revealed && isFilled) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "cellPop",
    )

    // Swap the face in at the halfway point of the flip, so the letter appears as
    // the tile turns edge-on rather than being visible through the back of it.
    val faceLetter = letter?.takeIf { flip < 45f }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationY = if (isFilled) flip else 0f
                scaleX = if (isFilled) pop else 1f
                scaleY = if (isFilled) pop else 1f
                cameraDistance = 12f * this.density
            }
            .background(
                color = when {
                    faceLetter != null && isHintRevealed -> colors.gridCellHinted
                    faceLetter != null -> colors.gridCellFilled
                    else -> colors.gridCellEmpty
                },
                shape = RoundedCornerShape(size * 0.18f),
            )
            .border(
                width = 1.dp,
                color = if (faceLetter != null) Color.Transparent else colors.gridCellBorder,
                shape = RoundedCornerShape(size * 0.18f),
            )
            .semantics {
                contentDescription = faceLetter?.let { "Letter $it" } ?: "Empty square"
            },
    ) {
        if (faceLetter != null) {
            Text(
                text = faceLetter.toString(),
                color = colors.gridCellText,
                fontSize = with(density) { (size.toPx() * 0.52f).toSp() },
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private val MIN_CELL = 20.dp

/**
 * Upper bound on cell size. Without a cap, a 3x3 level would inflate its cells to
 * fill the whole board area and dwarf the letter wheel; with one, small and large
 * levels keep a consistent visual weight.
 */
private val MAX_CELL = 62.dp
private const val REVEAL_STAGGER_MS = 55
private const val REVEAL_DURATION_MS = 260
