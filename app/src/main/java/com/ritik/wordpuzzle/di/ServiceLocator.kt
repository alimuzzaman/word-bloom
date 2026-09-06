package com.ritik.wordpuzzle.di

import android.content.Context
import com.ritik.wordpuzzle.data.local.DataStoreProgressRepository
import com.ritik.wordpuzzle.data.local.ProgressRepository
import com.ritik.wordpuzzle.data.local.DataStoreStoryReadingModeRepository
import com.ritik.wordpuzzle.data.local.StoryReadingModeRepository
import com.ritik.wordpuzzle.data.repository.LevelRepository
import com.ritik.wordpuzzle.data.repository.RemoteLevelRepository

/**
 * Minimal manual DI container.
 *
 * A full DI framework (Hilt) would be justified in a larger app, but this project has
 * exactly two singletons and three ViewModels — a hand-rolled locator keeps the
 * dependency graph obvious and the build free of annotation processing. The
 * ViewModels still receive their dependencies through constructors, so they remain
 * unit-testable with fakes regardless of how the app wires them at runtime.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Remote content is opt-in; the repository always falls back to bundled assets. */
    val levelRepository: LevelRepository by lazy { RemoteLevelRepository(appContext) }
    val progressRepository: ProgressRepository by lazy { DataStoreProgressRepository(appContext) }
    val storyReadingModeRepository: StoryReadingModeRepository by lazy {
        DataStoreStoryReadingModeRepository(appContext)
    }
}
