package com.example.studydeck.viewmodel

import com.example.studydeck.model.Deck
import com.example.studydeck.model.Flashcard

// State of the deck list
data class DeckListState(
    val isLoading: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val error: String? = null
)

// State of the card list of one deck (deckId tells which deck the cards belong to)
data class FlashcardListState(
    val deckId: String? = null,
    val isLoading: Boolean = false,
    val cards: List<Flashcard> = emptyList(),
    val error: String? = null
)

// State of loading ONE deck or ONE card (used by the edit screens)
sealed interface ItemState<out T> {
    object Idle : ItemState<Nothing>
    object Loading : ItemState<Nothing>
    data class Loaded<T>(val item: T) : ItemState<T>
    data class Error(val message: String) : ItemState<Nothing>
}
