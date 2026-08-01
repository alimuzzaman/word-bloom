package com.ritik.wordpuzzle.ui.home

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

data class HomeUiState(
    val continueLevelId: Int = 1,
    val completedCount: Int = 0,
    val totalLevels: Int = 0,
    val totalBonusWords: Int = 0,
) {
    val hasProgress: Boolean get() = completedCount > 0
}

/**
 * Supplies the home screen with progress-derived state.
 *
 * Collects the progress [kotlinx.coroutines.flow.Flow] for the ViewModel's lifetime
 * so returning from a finished level shows updated stats without a manual refresh.
 */
class HomeViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val total = levelRepository.levelCount()
            progressRepository.progress.collect { progress ->
                _state.update {
                    it.copy(
                        // Clamp so a fully-finished game re-opens on the last level
                        // rather than a level id that does not exist.
                        continueLevelId = progress.highestUnlockedLevel.coerceIn(1, total.coerceAtLeast(1)),
                        completedCount = progress.completedLevels.size,
                        totalLevels = total,
                        totalBonusWords = progress.totalBonusWords,
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
                return HomeViewModel(levelRepository, progressRepository) as T
            }
        }
    }
}
