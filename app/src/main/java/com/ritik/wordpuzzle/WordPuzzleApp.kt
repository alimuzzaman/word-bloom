package com.ritik.wordpuzzle

import android.app.Application
import com.ritik.wordpuzzle.di.AppContainer

/**
 * Owns the application-scoped dependency graph.
 *
 * The container lives here rather than in a global `object` so it is tied to the
 * Application lifecycle and holds an application Context — never an Activity one,
 * which would leak on rotation.
 */
class WordPuzzleApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
