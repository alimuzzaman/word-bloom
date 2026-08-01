package com.ritik.wordpuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ritik.wordpuzzle.ui.navigation.WordPuzzleNavHost
import com.ritik.wordpuzzle.ui.theme.BrandDeep
import com.ritik.wordpuzzle.ui.theme.WordPuzzleTheme

/**
 * Single-Activity host.
 *
 * All navigation is Compose Navigation inside one Activity, so there is one back
 * stack rather than an Activity stack layered on a Fragment stack. The manifest
 * declares `configChanges` for orientation and friends, so a rotation does not even
 * recreate the Activity — and where the system does recreate it (process death),
 * the ViewModel's `SavedStateHandle` restores the board.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Swap the launch theme's splash drawable back to the plain brand colour so
        // the window background does not show through the Compose UI.
        setTheme(R.style.Theme_WordPuzzle)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as WordPuzzleApp).container

        setContent {
            WordPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrandDeep,
                ) {
                    WordPuzzleNavHost(
                        navController = rememberNavController(),
                        appContainer = appContainer,
                    )
                }
            }
        }
    }
}
