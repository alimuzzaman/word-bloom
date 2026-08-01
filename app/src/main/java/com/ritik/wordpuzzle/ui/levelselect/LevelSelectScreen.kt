package com.ritik.wordpuzzle.ui.levelselect

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritik.wordpuzzle.R
import com.ritik.wordpuzzle.di.AppContainer
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.InkOnLight
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

/**
 * Grid of levels with lock/complete state.
 *
 * Locked levels are rendered but non-interactive rather than hidden, so the player
 * can see what is ahead — standard for the genre and better for motivation.
 */
@Composable
fun LevelSelectRoute(
    appContainer: AppContainer,
    onLevelClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModel.factory(
            appContainer.levelRepository,
            appContainer.progressRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LevelSelectScreen(
        state = state,
        onLevelClick = onLevelClick,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun LevelSelectScreen(
    state: LevelSelectUiState,
    onLevelClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .systemBarsPadding(),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            IconCircleButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
            )
            Spacer(Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.level_select_title),
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${state.completedCount}/${state.levels.size}",
                color = AccentTeal,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            // Bottom padding so the final row clears the navigation bar.
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            itemsIndexed(
                items = state.levels,
                key = { _, item -> item.id },
            ) { _, item ->
                LevelTile(
                    item = item,
                    onClick = { if (item.isUnlocked) onLevelClick(item.id) },
                )
            }
        }
    }
}

@Composable
private fun LevelTile(
    item: LevelSelectItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && item.isUnlocked) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "levelTilePress",
    )

    val face: Color = when {
        item.isCompleted -> AccentTeal
        item.isUnlocked -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.04f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .background(color = face, shape = RoundedCornerShape(18.dp))
            .border(
                width = 1.5.dp,
                color = when {
                    item.isCompleted -> Color.Transparent
                    item.isUnlocked -> AccentAmber.copy(alpha = 0.55f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.isUnlocked,
                onClick = onClick,
            )
            .semantics {
                contentDescription = when {
                    item.isCompleted -> "Level ${item.id}, completed"
                    item.isUnlocked -> "Level ${item.id}"
                    else -> "Level ${item.id}, locked"
                }
            },
    ) {
        when {
            !item.isUnlocked -> Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${item.id}",
                    color = if (item.isCompleted) InkOnLight else colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${item.letterCount}",
                    color = if (item.isCompleted) {
                        InkOnLight.copy(alpha = 0.65f)
                    } else {
                        colors.textSecondary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (item.isCompleted) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(17.dp)
                    .background(InkOnLight.copy(alpha = 0.85f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

@Composable
internal fun IconCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconPress",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .scale(scale)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
