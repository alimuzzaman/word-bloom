package com.ritik.wordpuzzle.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The three ways a child can read a category's learning story. */
enum class StoryReadingMode {
    COMPACT,
    FULL_STORY,
    CHAPTERS,
}

/** Small durable preference store for the category reading-mode selector. */
interface StoryReadingModeRepository {
    val mode: Flow<StoryReadingMode>

    suspend fun setMode(mode: StoryReadingMode)
}

private val Context.storyReadingModeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "story_reading_mode",
)

class DataStoreStoryReadingModeRepository private constructor(
    private val dataStore: DataStore<Preferences>,
) : StoryReadingModeRepository {

    constructor(context: Context) : this(context.storyReadingModeDataStore)

    internal constructor(dataStore: DataStore<Preferences>, testOnly: Unit = Unit) : this(dataStore)

    override val mode: Flow<StoryReadingMode> = dataStore.data.map { preferences ->
        preferences[KEY_MODE]
            ?.let { stored -> StoryReadingMode.entries.firstOrNull { it.name == stored } }
            ?: StoryReadingMode.COMPACT
    }

    override suspend fun setMode(mode: StoryReadingMode) {
        dataStore.edit { preferences -> preferences[KEY_MODE] = mode.name }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("category_story_reading_mode")
    }
}
