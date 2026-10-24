package com.example.studydeck.ui.theme

import com.example.studydeck.model.Deck
import com.example.studydeck.model.Flashcard


data class DeckListState(
    val isLoading: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val error: String? = null
)


data class FlashcardListState(
    val deckId: String? = null,
    val isLoading: Boolean = false,
    val cards: List<Flashcard> = emptyList(),
    val error: String? = null
)


sealed interface ItemState<out T> {
    object Idle : ItemState<Nothing>
    object Loading : ItemState<Nothing>
    data class Loaded<T>(val item: T) : ItemState<T>
    data class Error(val message: String) : ItemState<Nothing>
}
