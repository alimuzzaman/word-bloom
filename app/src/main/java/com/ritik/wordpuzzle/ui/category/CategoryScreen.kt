package com.ritik.wordpuzzle.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritik.wordpuzzle.di.AppContainer
import com.ritik.wordpuzzle.ui.levelselect.IconCircleButton
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

@Composable
fun CategoryRoute(
    appContainer: AppContainer,
    onCategoryClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModel.factory(
            appContainer.levelRepository,
            appContainer.progressRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoryScreen(state, onCategoryClick, onBack, modifier)
}

@Composable
private fun CategoryScreen(
    state: CategoryUiState,
    onCategoryClick: (String) -> Unit,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            IconCircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Navigate back", onBack)
            Spacer(Modifier.size(14.dp))
            Column {
                Text("Categories", color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("বিষয় বেছে নিন", color = colors.textSecondary, fontSize = 14.sp)
            }
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentAmber)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryCard(category, onClick = { onCategoryClick(category.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(item: CategoryItem, onClick: () -> Unit) {
    val colors = WordPuzzleTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${item.nameEn}, ${item.completedLevels} of ${item.totalLevels} levels complete" }
            .padding(horizontal = 12.dp, vertical = 18.dp),
    ) {
        Text(iconFor(item.iconKey), fontSize = 34.sp)
        Text(item.nameEn, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(item.nameBn, color = colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Text(
            "${item.completedLevels}/${item.totalLevels}",
            color = if (item.completedLevels == item.totalLevels && item.totalLevels > 0) AccentTeal else AccentAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun iconFor(key: String): String = when (key) {
    "paw" -> "🐾"; "fish" -> "🐟"; "bird" -> "🐦"; "flower" -> "🌸"
    "fruit" -> "🍎"; "vegetable" -> "🥕"; "plant" -> "🌿"; "body" -> "🖐️"
    "palette" -> "🎨"; "family" -> "👨‍👩‍👧"; "school" -> "🏫"; "transport" -> "🚌"
    "weather" -> "🌦️"; "kitchen" -> "🍳"; "clothing" -> "👕"; "numbers" -> "🔢"
    else -> "✦"
}
