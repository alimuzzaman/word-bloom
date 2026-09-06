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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ritik.wordpuzzle.data.local.StoryReadingMode
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.InkOnLight
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import com.ritik.wordpuzzle.ui.learning.CategoryGlossaryCard
import com.ritik.wordpuzzle.ui.learning.CategoryLearningCard
import com.ritik.wordpuzzle.ui.learning.LearningStoryModal
import com.ritik.wordpuzzle.ui.learning.dedupeLearningWords
import com.ritik.wordpuzzle.ui.learning.mergeStory
import com.ritik.wordpuzzle.ui.learning.StoryParagraphs
import com.ritik.wordpuzzle.ui.learning.CategoryStoryChapterData
import kotlinx.coroutines.launch

/**
 * Grid of levels with lock/complete state.
 *
 * Locked levels are rendered but non-interactive rather than hidden, so the player
 * can see what is ahead — standard for the genre and better for motivation.
 */
@Composable
fun LevelSelectRoute(
    categoryId: String,
    appContainer: AppContainer,
    onLevelClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModel.factory(
            categoryId,
            appContainer.levelRepository,
            appContainer.progressRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val readingMode by appContainer.storyReadingModeRepository.mode.collectAsStateWithLifecycle(
        initialValue = StoryReadingMode.COMPACT,
    )
    val scope = rememberCoroutineScope()

    LevelSelectScreen(
        state = state,
        readingMode = readingMode,
        onReadingModeChange = { mode ->
            scope.launch { appContainer.storyReadingModeRepository.setMode(mode) }
        },
        onLevelClick = onLevelClick,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun LevelSelectScreen(
    state: LevelSelectUiState,
    readingMode: StoryReadingMode,
    onReadingModeChange: (StoryReadingMode) -> Unit,
    onLevelClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    var showCategoryModal by rememberSaveable { mutableStateOf(false) }
    var showChapterPicker by rememberSaveable { mutableStateOf(false) }
    var selectedChapterId by rememberSaveable { mutableStateOf(-1) }
    val categoryComplete = state.completedCount == state.levels.size && state.levels.isNotEmpty()
    val selectedChapter = state.storyChapters.firstOrNull { it.levelId == selectedChapterId }
    val mergedStory = mergeStory(
        introductionEn = state.introductionEn,
        introductionBn = state.introductionBn,
        storyEn = state.storyEn,
        storyBn = state.storyBn,
    )

    fun closeCategoryModal() {
        showCategoryModal = false
        showChapterPicker = false
        selectedChapterId = -1
    }

    fun openCategoryStory() {
        showChapterPicker = false
        selectedChapterId = -1
        showCategoryModal = true
    }

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
                text = state.categoryNameEn.ifBlank { stringResource(R.string.level_select_title) },
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

        if (state.categoryNameBn.isNotBlank()) {
            Text(
                text = state.categoryNameBn,
                color = colors.textSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 72.dp),
            )
        }

        CategoryLearningCard(
            categoryNameEn = state.categoryNameEn,
            categoryNameBn = state.categoryNameBn,
            introductionEn = state.introductionEn,
            introductionBn = state.introductionBn,
            completed = categoryComplete,
            onReadStory = ::openCategoryStory,
            hasStory = state.storyEn.isNotBlank() || state.storyBn.isNotBlank(),
        )

        StoryModeSelector(
            selected = readingMode,
            onSelected = { mode ->
                onReadingModeChange(mode)
                when (mode) {
                    StoryReadingMode.CHAPTERS -> {
                        selectedChapterId = -1
                        showChapterPicker = true
                        showCategoryModal = true
                    }
                    StoryReadingMode.FULL_STORY -> openCategoryStory()
                    StoryReadingMode.COMPACT -> closeCategoryModal()
                }
            },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            // Bottom padding so the final row clears the navigation bar.
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Levels · স্তরসমূহ (${state.completedCount}/${state.levels.size})",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
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

    if (showCategoryModal) {
        val modalTitle = when {
            selectedChapter != null -> "Chapter ${selectedChapter.levelId} · ${selectedChapter.titleEn}"
            showChapterPicker -> "Story chapters · গল্পের অধ্যায়"
            else -> "${state.categoryNameEn} story · ${state.categoryNameBn}"
        }
        val modalKey = when {
            selectedChapter != null -> "chapter-${selectedChapter.levelId}"
            showChapterPicker -> "chapter-picker"
            else -> "category-story-${state.categoryNameEn}"
        }
        LearningStoryModal(
            visible = true,
            title = modalTitle,
            subtitle = if (selectedChapter != null) {
                selectedChapter.titleBn
            } else {
                "One story, then one glossary · একটি গল্প, তারপর একটি শব্দতালিকা"
            },
            contentKey = modalKey,
            onDismiss = ::closeCategoryModal,
        ) {
            when {
                selectedChapter != null -> {
                    StoryParagraphs(selectedChapter.passageEn, selectedChapter.passageBn)
                    if (selectedChapter.words.isNotEmpty()) {
                        CategoryGlossaryCard(words = dedupeLearningWords(selectedChapter.words))
                    }
                }
                showChapterPicker -> {
                    Text(
                        text = "Choose an unlocked chapter · খোলা অধ্যায় বেছে নিন",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    state.storyChapters.forEach { chapter ->
                        CategoryChapterPickerCard(
                            chapter = chapter,
                            onClick = if (chapter.isUnlocked) {
                                {
                                    selectedChapterId = chapter.levelId
                                    showChapterPicker = false
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
                else -> {
                    if (categoryComplete && (state.completionEn.isNotBlank() || state.completionBn.isNotBlank())) {
                        Text(
                            text = "✦ ${state.categoryNameEn} complete · সম্পূর্ণ",
                            color = AccentTeal,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                        )
                        if (state.completionEn.isNotBlank()) {
                            Text(
                                text = state.completionEn,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                        if (state.completionBn.isNotBlank()) {
                            Text(
                                text = state.completionBn,
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                    Text(
                        text = "Category story · বিষয়ভিত্তিক গল্প",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    StoryParagraphs(mergedStory.english, mergedStory.bengali)
                    if (state.storyWords.isNotEmpty()) {
                        Text(
                            text = "Words in this story · গল্পের শব্দ: ${state.storyWords.joinToString(" · ")}",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    CategoryGlossaryCard(words = dedupeLearningWords(state.storyGlossary))
                    if (state.completionWords.isNotEmpty()) {
                        Text(
                            text = "Words learned · শেখা শব্দ: ${state.completionWords.joinToString(" · ")}",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChapterPickerCard(
    chapter: CategoryStoryChapterData,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val enabled = onClick != null && chapter.isUnlocked
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = if (enabled) 0.08f else 0.045f),
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled, onClick = { onClick?.invoke() })
            .semantics {
                contentDescription = when {
                    !chapter.isUnlocked -> "Chapter ${chapter.levelId}, locked"
                    chapter.isCompleted -> "Chapter ${chapter.levelId}, completed, open story"
                    else -> "Chapter ${chapter.levelId}, open story"
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = if (chapter.isUnlocked) Icons.Default.Check else Icons.Default.Lock,
            contentDescription = null,
            tint = if (chapter.isUnlocked) AccentTeal else colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Chapter ${chapter.levelId} · ${chapter.titleEn}",
                color = if (enabled) colors.textPrimary else colors.textSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            if (chapter.titleBn.isNotBlank()) {
                Text(
                    text = chapter.titleBn,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            }
        }
        Text(
            text = when {
                chapter.isCompleted -> "Completed · সম্পন্ন"
                chapter.isUnlocked -> "Read · পড়ুন"
                else -> "Locked · তালাবদ্ধ"
            },
            color = if (chapter.isUnlocked) AccentTeal else AccentAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StoryModeSelector(
    selected: StoryReadingMode,
    onSelected: (StoryReadingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val options = listOf(
        StoryReadingMode.COMPACT to "Compact\nসংক্ষিপ্ত",
        StoryReadingMode.FULL_STORY to "Full story\nপুরো গল্প",
        StoryReadingMode.CHAPTERS to "Chapters\nঅধ্যায়",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics { contentDescription = "Story reading mode selector" },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "Reading mode · পড়ার ধরন",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (mode, label) ->
                val isSelected = mode == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) AccentTeal.copy(alpha = 0.23f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AccentTeal else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable(onClick = { onSelected(mode) })
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = if (isSelected) "$label, selected" else label
                        },
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 15.sp,
                    )
                }
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
