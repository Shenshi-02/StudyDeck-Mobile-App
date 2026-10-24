package com.example.studydeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.studydeck.navigation.StudyDeckNavGraph
import com.example.studydeck.ui.theme.StudyDeckTheme
import com.example.studydeck.viewmodel.AuthViewModel
import com.example.studydeck.viewmodel.DeckViewModel
import com.example.studydeck.viewmodel.FlashcardViewModel
import com.example.studydeck.viewmodel.StudyDeckFactory

class MainActivity : ComponentActivity() {

    private val factory by lazy {
        val app = application as StudyDeckApplication
        StudyDeckFactory(app.repository)
    }

    private val authViewModel: AuthViewModel by viewModels { factory }
    private val deckViewModel: DeckViewModel by viewModels { factory }
    private val flashcardViewModel: FlashcardViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudyDeckNavGraph(
                        authViewModel = authViewModel,
                        deckViewModel = deckViewModel,
                        flashcardViewModel = flashcardViewModel
                    )
                }
            }
        }
    }
}
