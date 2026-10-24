package com.example.studydeck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studydeck.data.FirebaseRepository

class StudyDeckFactory(
    private val repository: FirebaseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(repository) as T
            modelClass.isAssignableFrom(DeckViewModel::class.java) ->
                DeckViewModel(repository) as T
            modelClass.isAssignableFrom(FlashcardViewModel::class.java) ->
                FlashcardViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
