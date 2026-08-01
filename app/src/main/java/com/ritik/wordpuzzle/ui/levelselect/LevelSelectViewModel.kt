package com.ritik.wordpuzzle.ui.levelselect

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ritik.wordpuzzle.data.local.ProgressRepository
import com.ritik.wordpuzzle.data.repository.LevelRepository
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
)

/**
 * Joins the static level catalogue with dynamic progress.
 *
 * The catalogue is read once; progress is collected continuously, so completing a
 * level and pressing back updates the grid immediately.
 */
class LevelSelectViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LevelSelectUiState())
    val state: StateFlow<LevelSelectUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val levels = levelRepository.getLevels()
            progressRepository.progress.collect { progress ->
                _state.update {
                    LevelSelectUiState(
                        isLoading = false,
                        levels = levels.map { level ->
                            LevelSelectItem(
                                id = level.id,
                                letterCount = level.letters.size,
                                isUnlocked = level.id <= progress.highestUnlockedLevel,
                                isCompleted = level.id in progress.completedLevels,
                            )
                        },
                        completedCount = progress.completedLevels.size,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            levelRepository: LevelRepository,
            progressRepository: ProgressRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return LevelSelectViewModel(levelRepository, progressRepository) as T
            }
        }
    }
}
