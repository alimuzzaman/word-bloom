package com.ritik.wordpuzzle.ui.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritik.wordpuzzle.ui.theme.AccentAmber
import com.ritik.wordpuzzle.ui.theme.AccentTeal
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme
import com.ritik.wordpuzzle.ui.common.SecondaryButton

/**
 * UI-safe copy for one discovered word.
 *
 * This is deliberately separate from the catalog/domain DTO. Remote catalogs and
 * older bundled catalogs can be mapped to this small presentation shape without
 * making the composables know how content was sourced.
 */
@Immutable
data class LearningWordCardData(
    val word: String,
    val definitionEn: String,
    val translationBn: String,
    val meaningBn: String,
    /** The authored level sentence that uses this word, when available. */
    val exampleEn: String = "",
    val exampleBn: String = "",
)

@Immutable
data class LessonCardData(
    val heroWord: LearningWordCardData,
    val sentenceEn: String,
    val sentenceBn: String,
    val usedWords: List<String> = emptyList(),
)

/** One progressive chapter in the category reading view. */
@Immutable
data class CategoryStoryChapterData(
    val levelId: Int,
    val titleEn: String,
    val titleBn: String,
    val passageEn: String,
    val passageBn: String,
    val words: List<LearningWordCardData> = emptyList(),
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
)

/** Split authored copy on blank lines while preserving line breaks inside paragraphs. */
internal fun storyParagraphs(story: String): List<String> =
    story.trim()
        .split(Regex("\\r?\\n\\s*\\r?\\n"))
        .map(String::trim)
        .filter(String::isNotBlank)

/**
 * Compact category introduction shown above the level list.
 *
 * The card starts expanded so a child sees context before the first puzzle. It can
 * be collapsed to keep the level grid easy to scan on subsequent visits.
 */
@Composable
fun CategoryIntroCard(
    categoryNameEn: String,
    categoryNameBn: String,
    introductionEn: String,
    introductionBn: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (introductionEn.isBlank() && introductionBn.isBlank()) return

    val colors = WordPuzzleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(colors.surfaceElevated.copy(alpha = 0.96f), colors.surface.copy(alpha = 0.94f)),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .semantics {
                contentDescription = "$categoryNameEn, $categoryNameBn, category introduction"
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = AccentAmber,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = categoryNameEn,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (categoryNameBn.isNotBlank()) {
                    Text(
                        text = categoryNameBn,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide introduction" else "Show introduction",
                tint = colors.textSecondary,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (introductionEn.isNotBlank()) {
                    Text(
                        text = introductionEn,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    )
                }
                if (introductionBn.isNotBlank()) {
                    Text(
                        text = introductionBn,
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}

/**
 * Short category lead shown above the level grid. Long reading content lives in
 * [LearningStoryModal], so this card never becomes a second scroll surface.
 */
@Composable
fun CategoryLearningCard(
    categoryNameEn: String,
    categoryNameBn: String,
    introductionEn: String,
    introductionBn: String,
    hasStory: Boolean = false,
    completed: Boolean,
    onReadStory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (introductionEn.isBlank() && introductionBn.isBlank() && !hasStory && !completed) return

    val colors = WordPuzzleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(colors.surfaceElevated.copy(alpha = 0.96f), colors.surface.copy(alpha = 0.94f)),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .semantics {
                contentDescription = "$categoryNameEn, $categoryNameBn, category learning"
            },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Learn $categoryNameEn · $categoryNameBn",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                if (completed) {
                    Text(
                        text = "Category complete · বিভাগ সম্পূর্ণ",
                        color = AccentTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (introductionEn.isNotBlank()) {
            Text(
                text = introductionEn,
                color = colors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
        if (introductionBn.isNotBlank()) {
            Text(
                text = introductionBn,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }
        SecondaryButton(
            text = "Read story and meanings · গল্প ও অর্থ পড়ুন",
            onClick = onReadStory,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Category story card used by the three reading modes.
 *
 * The card is retained for older callers, but its body is deliberately not
 * scrollable. Long content belongs in [LearningStoryModal], whose body owns the
 * one vertical scroll for the surface.
 */
@Composable
fun CategoryStoryCard(
    categoryNameEn: String,
    categoryNameBn: String,
    storyEn: String,
    storyBn: String,
    storyWords: List<String> = emptyList(),
    expanded: Boolean,
    onToggle: () -> Unit,
    onReadStory: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (storyEn.isBlank() && storyBn.isBlank()) return

    val colors = WordPuzzleTheme.colors
    val paragraphCount = maxOf(storyParagraphs(storyEn).size, storyParagraphs(storyBn).size)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(colors.surfaceElevated.copy(alpha = 0.97f), colors.surface.copy(alpha = 0.94f)),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .semantics {
                contentDescription = "$categoryNameEn, $categoryNameBn, category story, $paragraphCount paragraphs"
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Category story · বিষয়ভিত্তিক গল্প",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$categoryNameEn · $categoryNameBn",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = "$paragraphCount paragraphs · ${storyWords.size} words",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide category story" else "Show category story",
                tint = colors.textSecondary,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StoryParagraphs(storyEn = storyEn, storyBn = storyBn)
                if (storyWords.isNotEmpty()) {
                    Text(
                        text = "Words in this story · গল্পের শব্দ",
                        color = AccentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = storyWords.joinToString(" · "),
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                    )
                }
                if (onReadStory != null) {
                    SecondaryButton(
                        text = "Open full story · পুরো গল্প",
                        onClick = onReadStory,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // In compact mode the story stays collapsed, so the CTA remains visible
        // without requiring a child to expand a dense card first.
        if (!expanded && onReadStory != null) {
            SecondaryButton(
                text = "Read story · গল্প পড়ুন",
                onClick = onReadStory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/** Full glossary for a category story. The parent modal/page owns scrolling. */
@Composable
fun CategoryGlossaryCard(
    words: List<LearningWordCardData>,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) return

    val colors = WordPuzzleTheme.colors
    val uniqueWords = dedupeLearningWords(words)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = "Complete glossary · সম্পূর্ণ শব্দতালিকা",
            color = colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Every story word has a meaning in English and Bengali.",
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            uniqueWords.forEach { word -> LearningWordRow(word) }
        }
    }
}

/** One chapter in the progressive reading mode. */
@Composable
fun CategoryStoryChapterCard(
    chapter: CategoryStoryChapterData,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    val status = when {
        chapter.isCompleted -> "Completed · সম্পন্ন"
        chapter.isUnlocked -> "Ready · প্রস্তুত"
        else -> "Locked · তালাবদ্ধ"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = if (chapter.isUnlocked) 0.08f else 0.045f),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Chapter ${chapter.levelId} · ${chapter.titleEn}",
                    color = if (chapter.isUnlocked) colors.textPrimary else colors.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (chapter.titleBn.isNotBlank()) {
                    Text(text = chapter.titleBn, color = colors.textSecondary, fontSize = 13.sp)
                }
            }
            Text(
                text = status,
                color = if (chapter.isCompleted) AccentTeal else AccentAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (chapter.isUnlocked) {
            StoryParagraphs(chapter.passageEn, chapter.passageBn)
            if (chapter.words.isNotEmpty()) {
                Text(
                    text = "Chapter words · অধ্যায়ের শব্দ",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                chapter.words.forEach { LearningWordRow(it) }
            }
        } else {
            Text(
                text = "Complete Level ${chapter.levelId} to unlock this chapter.\nলেভেল ${chapter.levelId} শেষ করলে এই অধ্যায় খুলবে।",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
internal fun StoryParagraphs(
    storyEn: String,
    storyBn: String,
) {
    val colors = WordPuzzleTheme.colors
    if (storyEn.isNotBlank()) {
        Text(
            text = "English · ইংরেজি",
            color = AccentAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        storyParagraphs(storyEn).forEach { paragraph ->
            Text(
                text = paragraph,
                color = colors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 23.sp,
            )
        }
    }
    if (storyBn.isNotBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = "বাংলা · Bengali",
            color = AccentAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        storyParagraphs(storyBn).forEach { paragraph ->
            Text(
                text = paragraph,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 23.sp,
            )
        }
    }
}

/** Bilingual category story shown after all levels in a category are complete. */
@Composable
fun CategoryCompletionCard(
    categoryNameEn: String,
    categoryNameBn: String,
    completionEn: String,
    completionBn: String,
    completionWords: List<String> = emptyList(),
    storyEn: String = "",
    storyBn: String = "",
    storyWords: List<String> = emptyList(),
    storyGlossary: List<LearningWordCardData> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (completionEn.isBlank() && completionBn.isBlank() && storyEn.isBlank() && storyBn.isBlank()) return

    val colors = WordPuzzleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                Brush.verticalGradient(listOf(AccentTeal.copy(alpha = 0.2f), colors.surface.copy(alpha = 0.95f))),
                RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 18.dp, vertical = 17.dp)
            .semantics { contentDescription = "$categoryNameEn, category story" },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "✦ $categoryNameEn complete · সম্পূর্ণ",
            color = AccentTeal,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
        if (categoryNameBn.isNotBlank()) {
            Text(
                text = categoryNameBn,
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
        }
        if (completionEn.isNotBlank()) {
            Text(text = completionEn, color = colors.textPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        }
        if (completionBn.isNotBlank()) {
            Text(text = completionBn, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
        if (completionWords.isNotEmpty()) {
            Text(
                text = "Words learned / শেখা শব্দ: ${completionWords.joinToString(" · ")}",
                color = AccentTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (storyEn.isNotBlank() || storyBn.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = "The whole story · পুরো গল্প",
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StoryParagraphs(storyEn, storyBn)
                if (storyWords.isNotEmpty()) {
                    Text(
                        text = "Words in this story · গল্পের শব্দ: ${storyWords.joinToString(" · ")}",
                        color = AccentTeal,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (storyGlossary.isNotEmpty()) {
            CategoryGlossaryCard(words = storyGlossary)
        }
    }
}

/**
 * Lesson card used in the level-complete moment.
 *
 * It gives a short sentence in both languages. Hero metadata remains in the data
 * shape for compatibility, but the hero definition is rendered once in the
 * glossary below rather than repeated here.
 */
@Composable
fun LevelLessonCard(
    lesson: LessonCardData,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Level story · স্তরের গল্প",
            color = AccentTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        if (lesson.sentenceEn.isNotBlank()) {
            Text(
                text = lesson.sentenceEn,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
            )
        }
        if (lesson.sentenceBn.isNotBlank()) {
            Text(
                text = lesson.sentenceBn,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
        if (lesson.usedWords.isNotEmpty()) {
            Text(
                text = "Words learned / শেখা শব্দ: ${lesson.usedWords.joinToString(" · ")}",
                color = AccentTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Shared story + glossary body for level meanings and completion.
 *
 * Callers pass the currently visible glossary entries. The story's word index is
 * narrowed to that same set, so unfinished levels cannot leak unsolved answers.
 */
@Composable
fun LevelLearningContent(
    lesson: LessonCardData?,
    words: List<LearningWordCardData>,
    glossaryTitle: String,
    emptyText: String,
    showDetailedWords: Boolean = false,
) {
    val uniqueWords = dedupeLearningWords(words)
    if (lesson != null) {
        // Reuse the same visibility boundary for the story's word index. This
        // prevents an active level from leaking every target through `usedWords`.
        LevelLessonCard(restrictLessonWords(lesson, uniqueWords) ?: lesson)
    }
    Spacer(Modifier.height(4.dp))
    Text(
        text = glossaryTitle,
        color = AccentTeal,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
    if (uniqueWords.isEmpty()) {
        Text(
            text = emptyText,
            color = WordPuzzleTheme.colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )
    } else {
        uniqueWords.forEach { word -> LearningWordRow(word, showDetails = showDetailedWords) }
    }
}

/** One meaning row inside the found-word guide. */
@Composable
private fun LearningWordRow(
    word: LearningWordCardData,
    showDetails: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = WordPuzzleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.word,
                color = AccentAmber,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            if (word.translationBn.isNotBlank()) {
                Text(
                    text = "  ${word.translationBn}",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
            }
        }
        if (showDetails) {
            if (word.definitionEn.isNotBlank()) {
                Text(
                    text = "English meaning · ইংরেজি অর্থ",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = word.definitionEn, color = colors.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            }
            if (word.meaningBn.isNotBlank()) {
                Text(
                    text = "বাংলা অর্থ · Bengali meaning",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = word.meaningBn, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
            if (word.exampleEn.isNotBlank() || word.exampleBn.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Example from this level · এই স্তরের উদাহরণ",
                    color = AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (word.exampleEn.isNotBlank()) {
                    Text(text = word.exampleEn, color = colors.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                }
                if (word.exampleBn.isNotBlank()) {
                    Text(text = word.exampleBn, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        } else {
            if (word.definitionEn.isNotBlank()) {
                Text(text = word.definitionEn, color = colors.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            }
            if (word.meaningBn.isNotBlank()) {
                Text(text = word.meaningBn, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

/**
 * Near-full-height guide for meanings in a level.
 *
 * The book button in the game top bar opens this sheet. During an active level it
 * is a found-word aid; the completion modal owns the full glossary after the
 * player solves the level.
 */
@Composable
fun FoundWordsOverlay(
    visible: Boolean,
    levelLabel: String,
    foundWords: List<LearningWordCardData>,
    lesson: LessonCardData? = null,
    allWords: List<LearningWordCardData> = emptyList(),
    showAllWords: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wordsToShow = selectLearningWords(
        foundWords = foundWords.map { it.word },
        allWords = allWords.ifEmpty { foundWords },
        showAllWords = showAllWords,
    )
    LearningStoryModal(
        visible = visible,
        title = "Word meanings · শব্দের অর্থ",
        subtitle = levelLabel,
        contentKey = levelLabel,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        LevelLearningContent(
            lesson = lesson,
            words = wordsToShow,
            glossaryTitle = if (showAllWords) {
                "All target words · সব লক্ষ্যশব্দ"
            } else {
                "Found words · পাওয়া শব্দ"
            },
            emptyText = if (showAllWords) {
                "This level has no word meanings yet.\nএই স্তরে এখনও কোনো শব্দের অর্থ নেই।"
            } else {
                "Find a word to see its meaning.\nএকটি শব্দ খুঁজে তার অর্থ দেখুন।"
            },
            showDetailedWords = showAllWords,
        )
    }
}
