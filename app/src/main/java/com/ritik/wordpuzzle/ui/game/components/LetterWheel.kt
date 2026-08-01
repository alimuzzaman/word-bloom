package com.ritik.wordpuzzle.ui.game.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ritik.wordpuzzle.domain.model.LetterTile
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The circular letter wheel and its continuous swipe gesture.
 *
 * ## Why one gesture loop instead of per-tile pointer handlers
 * Each tile could own a `pointerInput` and report its own touches, but Compose
 * delivers a pointer stream to the composable where the gesture *started*: once the
 * finger leaves the first tile, that tile keeps receiving the events and its
 * neighbours never hear about them. So the whole wheel owns a single
 * [awaitEachGesture] loop and does its own hit-testing against tile centres. That
 * also makes "drag back to undo" and rapid flicks tractable, because one place sees
 * the entire finger path.
 *
 * ## Sampling the path, not just the events
 * Fast swipes produce sparse move events — a flick can jump 200px between frames and
 * skip straight over a tile. Rather than hit-test only at event positions, we
 * interpolate along the segment between the previous and current point and test at
 * intervals, so a tile between two samples is still registered. This is what makes
 * rapid swiping behave (an explicit evaluation criterion).
 *
 * ## Rendering
 * Tiles are real composables (so text, elevation and per-tile animation come for
 * free), while the trail and wheel face are drawn on [Canvas] beneath them. Tile
 * centres are computed once per layout and shared by both the renderer and the
 * hit-tester, so what the player sees is exactly what they can hit.
 */
@Composable
fun LetterWheel(
    tiles: List<LetterTile>,
    selection: List<LetterTile>,
    onSelectionStart: (LetterTile) -> Unit,
    onSelectionExtend: (LetterTile) -> Unit,
    onSelectionCommit: () -> Unit,
    onSelectionCancel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tileSize: Dp = 58.dp,
) {
    val colors = WordPuzzleTheme.colors
    val density = LocalDensity.current

    // aspectRatio keeps the wheel circular whether the caller gives it a fixed size
    // or lets it fill the available width.
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val diameterPx = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val tileRadiusPx = with(density) { tileSize.toPx() } / 2f
        val centre = Offset(diameterPx / 2f, diameterPx / 2f)

        // Ring radius leaves a tile-radius margin so tiles sit fully inside the wheel.
        val ringRadius = (diameterPx / 2f) - tileRadiusPx - with(density) { 8.dp.toPx() }

        // Tile centres, recomputed only when the tile set or geometry changes.
        // A single tile is centred; otherwise they distribute evenly from 12 o'clock.
        val tileCentres = remember(tiles, ringRadius, centre) {
            if (tiles.size == 1) {
                listOf(centre)
            } else {
                tiles.mapIndexed { index, _ ->
                    val angle = (-PI / 2) + (2 * PI * index / tiles.size)
                    Offset(
                        x = centre.x + (ringRadius * cos(angle)).toFloat(),
                        y = centre.y + (ringRadius * sin(angle)).toFloat(),
                    )
                }
            }
        }

        // Live finger position; drives the rubber-band segment from the last tile.
        var fingerPosition by remember { mutableStateOf<Offset?>(null) }

        // rememberUpdatedState so the long-lived gesture coroutine always calls the
        // latest lambdas without being torn down and restarted on recomposition.
        val currentTiles by rememberUpdatedState(tiles)
        val currentSelection by rememberUpdatedState(selection)
        val currentEnabled by rememberUpdatedState(enabled)
        val onStart by rememberUpdatedState(onSelectionStart)
        val onExtend by rememberUpdatedState(onSelectionExtend)
        val onCommit by rememberUpdatedState(onSelectionCommit)
        val onCancel by rememberUpdatedState(onSelectionCancel)

        // Generous hit radius: fingertips are imprecise and the visual tile is small.
        val hitRadius = tileRadiusPx * HIT_RADIUS_FACTOR

        fun tileAt(position: Offset): LetterTile? {
            var best: LetterTile? = null
            var bestDistance = Float.MAX_VALUE
            tileCentres.forEachIndexed { index, tileCentre ->
                val distance = (position - tileCentre).getDistance()
                // Nearest wins, so overlapping hit circles resolve predictably.
                if (distance <= hitRadius && distance < bestDistance) {
                    bestDistance = distance
                    best = currentTiles.getOrNull(index)
                }
            }
            return best
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                // The wheel face and swipe trail render behind the tile composables.
                .drawBehind {
                    drawWheelFace(centre, ringRadius + tileRadiusPx + 6.dp.toPx(), colors.wheelFace, colors.wheelRing)
                    drawTrail(
                        selection = currentSelection,
                        tiles = currentTiles,
                        tileCentres = tileCentres,
                        fingerPosition = fingerPosition,
                        color = colors.trail,
                        strokeWidth = tileRadiusPx * 0.42f,
                    )
                }
                .pointerInput(tiles, enabled) {
                    if (!currentEnabled) return@pointerInput
                    awaitEachGesture {
                        // --- finger down -------------------------------------------------
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startTile = tileAt(down.position)
                        if (startTile == null) {
                            // Touch started off any tile: ignore this gesture entirely
                            // rather than starting a word the player did not intend.
                            return@awaitEachGesture
                        }
                        down.consume()
                        fingerPosition = down.position
                        onStart(startTile)

                        var previous = down.position
                        var pointerId = down.id

                        // --- finger moves ------------------------------------------------
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change: PointerInputChange =
                                event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull { it.pressed }
                                    ?: break
                            pointerId = change.id

                            if (!change.pressed) {
                                // --- finger up: commit the traced word -------------------
                                change.consume()
                                fingerPosition = null
                                onCommit()
                                break
                            }

                            val position = change.position
                            fingerPosition = position

                            // Interpolate between the last and current sample so a fast
                            // flick cannot skip a tile that lies between two events.
                            val distance = (position - previous).getDistance()
                            val steps = ((distance / (hitRadius * SAMPLE_FRACTION)).toInt() + 1)
                                .coerceAtMost(MAX_INTERPOLATION_STEPS)
                            for (step in 1..steps) {
                                val t = step.toFloat() / steps
                                val sample = Offset(
                                    x = previous.x + (position.x - previous.x) * t,
                                    y = previous.y + (position.y - previous.y) * t,
                                )
                                tileAt(sample)?.let(onExtend)
                            }

                            previous = position
                            change.consume()
                        }

                        // Safety net: if the loop exits without an up event (pointer
                        // cancelled by the system), drop the in-flight selection.
                        if (fingerPosition != null) {
                            fingerPosition = null
                            onCancel()
                        }
                    }
                },
        ) {
            // Tiles on top of the canvas layers.
            tiles.forEachIndexed { index, tile ->
                val tileCentre = tileCentres.getOrNull(index) ?: return@forEachIndexed
                val selectionIndex = selection.indexOfFirst { it.id == tile.id }
                key(tile.id) {
                    WheelTile(
                        letter = tile.char,
                        isSelected = selectionIndex >= 0,
                        centre = tileCentre,
                        size = tileSize,
                    )
                }
            }
        }
    }
}

/** Faint disc and ring the tiles sit on, so the wheel reads as one object. */
private fun DrawScope.drawWheelFace(
    centre: Offset,
    radius: Float,
    faceColor: Color,
    ringColor: Color,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(faceColor, Color.Transparent),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
    drawCircle(
        color = ringColor,
        radius = radius,
        center = centre,
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

/**
 * The swipe trail: a rounded polyline through selected tile centres, plus a live
 * segment to the finger. Drawn as one [Path] so the joins are smooth rather than a
 * chain of separate lines with visible seams.
 */
private fun DrawScope.drawTrail(
    selection: List<LetterTile>,
    tiles: List<LetterTile>,
    tileCentres: List<Offset>,
    fingerPosition: Offset?,
    color: Color,
    strokeWidth: Float,
) {
    if (selection.isEmpty()) return

    val points = selection.mapNotNull { selected ->
        val index = tiles.indexOfFirst { it.id == selected.id }
        tileCentres.getOrNull(index)
    }
    if (points.isEmpty()) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        // Rubber band to the finger, so the line always feels attached.
        fingerPosition?.let { lineTo(it.x, it.y) }
    }

    // Soft underglow, then the solid trail: cheap depth without a blur pass.
    drawPath(
        path = path,
        color = color.copy(alpha = 0.28f),
        style = Stroke(width = strokeWidth * 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Dot on the leading end so the trail has a defined head.
    points.lastOrNull()?.let { last ->
        val head = fingerPosition ?: last
        drawCircle(color = color, radius = strokeWidth * 0.62f, center = head)
    }
}

private const val HIT_RADIUS_FACTOR = 1.18f
private const val SAMPLE_FRACTION = 0.5f
private const val MAX_INTERPOLATION_STEPS = 24
