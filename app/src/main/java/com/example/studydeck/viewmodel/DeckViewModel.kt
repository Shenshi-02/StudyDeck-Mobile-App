package com.example.studydeck.viewmodel

import androidx.lifecycle.ViewModel
import com.example.studydeck.data.FirebaseRepository
import com.example.studydeck.model.Deck
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeckViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckListState())
    val uiState: StateFlow<DeckListState> = _uiState.asStateFlow()

    private val _deckState = MutableStateFlow<ItemState<Deck>>(ItemState.Idle)
    val deckState: StateFlow<ItemState<Deck>> = _deckState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var requestedDeckId: String? = null

    // Start listening to the logged-in user's decks (also used for "Try again")
    fun loadDecks() {
        listener?.remove()
        _uiState.value = DeckListState(isLoading = true)
        listener = repository.listenToDecks { decks, error ->
            _uiState.value = if (error != null) {
                DeckListState(error = error)
            } else {
                DeckListState(decks = decks)
            }
        }
    }

    // Called on logout
    fun clearDecks() {
        listener?.remove()
        listener = null
        _uiState.value = DeckListState()
        clearDeck()
    }

    fun loadDeck(deckId: String) {
        requestedDeckId = deckId
        _deckState.value = ItemState.Loading
        repository.getDeck(deckId) { deck, error ->
            if (requestedDeckId != deckId) return@getDeck
            _deckState.value = if (deck != null) {
                ItemState.Loaded(deck)
            } else {
                ItemState.Error(error ?: "Deck not found")
            }
        }
    }

    fun clearDeck() {
        requestedDeckId = null
        _deckState.value = ItemState.Idle
    }

    // onSuccess runs only after Firestore saved the deck
    fun createDeck(
        title: String,
        subject: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (title.isBlank()) {
            onError("Title is required")
            return
        }
        repository.createDeck(title, subject, description) { success, message ->
            if (success) onSuccess() else onError(message ?: "Could not save the deck")
        }
    }

    fun updateDeck(
        deckId: String,
        title: String,
        subject: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (title.isBlank()) {
            onError("Title is required")
            return
        }
        repository.updateDeck(deckId, title, subject, description) { success, message ->
            if (success) onSuccess() else onError(message ?: "Could not update the deck")
        }
    }

    fun deleteDeck(deck: Deck) {
        repository.deleteDeck(deck.id) { success, message ->
            if (!success) _actionError.value = message ?: "Could not delete the deck"
        }
    }

    fun dismissActionError() {
        _actionError.value = null
    }

    override fun onCleared() {
        listener?.remove()
    }
}
