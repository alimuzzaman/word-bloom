package com.ritik.wordpuzzle.ui.levelselect

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ritik.wordpuzzle.data.local.ProgressRepository
import com.ritik.wordpuzzle.data.repository.LevelRepository
import com.ritik.wordpuzzle.domain.model.Level
import com.ritik.wordpuzzle.ui.learning.CategoryStoryChapterData
import com.ritik.wordpuzzle.ui.learning.LearningWordCardData
import com.ritik.wordpuzzle.ui.learning.dedupeLearningWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One cell in the level grid. */
@Immutable
data class LevelSelectItem(
    val id: Int,
    val letterCount: Int,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
)

@Immutable
data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val levels: List<LevelSelectItem> = emptyList(),
    val completedCount: Int = 0,
    val categoryNameEn: String = "",
    val categoryNameBn: String = "",
    val introductionEn: String = "",
    val introductionBn: String = "",
    val completionEn: String = "",
    val completionBn: String = "",
    val completionWords: List<String> = emptyList(),
    val storyEn: String = "",
    val storyBn: String = "",
    val storyWords: List<String> = emptyList(),
    val storyChapters: List<CategoryStoryChapterData> = emptyList(),
    val storyGlossary: List<LearningWordCardData> = emptyList(),
)

/**
 * Joins the static level catalogue with dynamic progress.
 *
 * The catalogue is read once; progress is collected continuously, so completing a
 * level and pressing back updates the grid immediately.
 */
class LevelSelectViewModel(
    private val categoryId: String,
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LevelSelectUiState())
    val state: StateFlow<LevelSelectUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val category = levelRepository.getCategories().firstOrNull { it.id == categoryId }
            val levels = levelRepository.getLevels(categoryId)
            progressRepository.progress(categoryId).collect { progress ->
                val glossary = buildStoryGlossary(levels, category?.storyWords.orEmpty())
                _state.update {
                    LevelSelectUiState(
                        isLoading = false,
                        levels = levels.map { level ->
                            LevelSelectItem(
                                id = level.id,
                                letterCount = level.letters.size,
                                isUnlocked = level.order <= progress.highestUnlockedOrder,
                                isCompleted = level.id in progress.completedLevels,
                            )
                        },
                        completedCount = progress.completedLevels.size,
                        categoryNameEn = category?.nameEn.orEmpty(),
                        categoryNameBn = category?.nameBn.orEmpty(),
                        introductionEn = category?.introductionEn.orEmpty(),
                        introductionBn = category?.introductionBn.orEmpty(),
                        completionEn = category?.completionEn.orEmpty(),
                        completionBn = category?.completionBn.orEmpty(),
                        completionWords = category?.completionWords.orEmpty(),
                        storyEn = category?.storyEn.orEmpty(),
                        storyBn = category?.storyBn.orEmpty(),
                        storyWords = category?.storyWords.orEmpty(),
                        storyChapters = levels.map { level ->
                            level.toStoryChapter(
                                isUnlocked = level.order <= progress.highestUnlockedOrder,
                                isCompleted = level.id in progress.completedLevels,
                            )
                        },
                        storyGlossary = glossary,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            categoryId: String,
            levelRepository: LevelRepository,
            progressRepository: ProgressRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return LevelSelectViewModel(categoryId, levelRepository, progressRepository) as T
            }
        }
    }
}

private fun buildStoryGlossary(
    levels: List<Level>,
    storyWords: List<String>,
): List<LearningWordCardData> {
    val meaningsByWord = levels
        .flatMap { level -> level.wordMeanings }
        .associateBy { meaning -> meaning.word.uppercase() }
    val heroesByWord = levels.associateBy { level -> level.heroWord.word.uppercase() }
    val orderedWords = (storyWords + levels.flatMap { it.words })
        .map(String::uppercase)
        .distinct()

    return dedupeLearningWords(orderedWords.mapNotNull { word ->
        val meaning = meaningsByWord[word]
        when {
            meaning != null -> LearningWordCardData(
                word = meaning.word,
                definitionEn = meaning.definitionEn,
                translationBn = meaning.translationBn,
                meaningBn = meaning.meaningBn,
            )
            heroesByWord[word] != null -> heroesByWord.getValue(word).heroWord.let { hero ->
                LearningWordCardData(
                    word = hero.word,
                    definitionEn = hero.definitionEn,
                    translationBn = hero.translationBn,
                    meaningBn = hero.meaningBn,
                )
            }
            else -> null
        }
    })
}

private fun Level.toStoryChapter(
    isUnlocked: Boolean,
    isCompleted: Boolean,
): CategoryStoryChapterData = CategoryStoryChapterData(
    levelId = id,
    titleEn = heroWord.word,
    titleBn = heroWord.translationBn,
    passageEn = lesson?.sentenceEn.orEmpty(),
    passageBn = lesson?.sentenceBn.orEmpty(),
    words = wordMeanings.map { meaning ->
        LearningWordCardData(
            word = meaning.word,
            definitionEn = meaning.definitionEn,
            translationBn = meaning.translationBn,
            meaningBn = meaning.meaningBn,
        )
    },
    isUnlocked = isUnlocked,
    isCompleted = isCompleted,
)
