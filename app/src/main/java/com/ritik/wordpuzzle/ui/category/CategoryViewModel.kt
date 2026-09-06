package com.ritik.wordpuzzle.ui.category

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

@Immutable
data class CategoryItem(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val iconKey: String,
    val completedLevels: Int,
    val totalLevels: Int,
)

@Immutable
data class CategoryUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryItem> = emptyList(),
)

class CategoryViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = levelRepository.getCategories()
            val counts = categories.associate { it.id to levelRepository.levelCount(it.id) }
            progressRepository.allProgress.collect { progress ->
                _state.update {
                    CategoryUiState(
                        isLoading = false,
                        categories = categories.map { category ->
                            CategoryItem(
                                id = category.id,
                                nameEn = category.nameEn,
                                nameBn = category.nameBn,
                                iconKey = category.iconKey,
                                completedLevels = progress[category.id]?.completedLevels?.size ?: 0,
                                totalLevels = counts[category.id] ?: 0,
                            )
                        },
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
                return CategoryViewModel(levelRepository, progressRepository) as T
            }
        }
    }
}
