package com.example.studydeck.viewmodel

import androidx.lifecycle.ViewModel
import com.example.studydeck.data.FirebaseRepository
import com.example.studydeck.model.Flashcard
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Cards of the deck that is currently open
class FlashcardViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardListState())
    val uiState: StateFlow<FlashcardListState> = _uiState.asStateFlow()

    private val _cardState = MutableStateFlow<ItemState<Flashcard>>(ItemState.Idle)
    val cardState: StateFlow<ItemState<Flashcard>> = _cardState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var requestedCardId: String? = null

    // Start listening to the cards of a deck (also used for "Try again")
    fun loadCards(deckId: String) {
        val state = _uiState.value
        val alreadyListening = listener != null && state.deckId == deckId && state.error == null
        if (alreadyListening) return

        listener?.remove()
        _uiState.value = FlashcardListState(deckId = deckId, isLoading = true)
        listener = repository.listenToCards(deckId) { cards, error ->
            _uiState.value = if (error != null) {
                FlashcardListState(deckId = deckId, error = error)
            } else {
                FlashcardListState(deckId = deckId, cards = cards)
            }
        }
    }

    // Called on logout
    fun clearCards() {
        listener?.remove()
        listener = null
        _uiState.value = FlashcardListState()
        clearCard()
    }

    fun loadCard(deckId: String, cardId: String) {
        requestedCardId = cardId
        _cardState.value = ItemState.Loading
        repository.getCard(deckId, cardId) { card, error ->
            if (requestedCardId != cardId) return@getCard
            _cardState.value = if (card != null) {
                ItemState.Loaded(card)
            } else {
                ItemState.Error(error ?: "Flashcard not found")
            }
        }
    }

    fun clearCard() {
        requestedCardId = null
        _cardState.value = ItemState.Idle
    }

    // onSuccess runs only after Firestore saved the card
    fun addCard(
        deckId: String,
        question: String,
        answer: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (question.isBlank() || answer.isBlank()) {
            onError("Question and answer are required")
            return
        }
        repository.addCard(deckId, question, answer) { success, message ->
            if (success) onSuccess() else onError(message ?: "Could not save the card")
        }
    }

    fun updateCard(
        deckId: String,
        cardId: String,
        question: String,
        answer: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (question.isBlank() || answer.isBlank()) {
            onError("Question and answer are required")
            return
        }
        repository.updateCard(deckId, cardId, question, answer) { success, message ->
            if (success) onSuccess() else onError(message ?: "Could not update the card")
        }
    }

    fun deleteCard(deckId: String, card: Flashcard) {
        repository.deleteCard(deckId, card.id) { success, message ->
            if (!success) _actionError.value = message ?: "Could not delete the card"
        }
    }

    fun dismissActionError() {
        _actionError.value = null
    }

    override fun onCleared() {
        listener?.remove()
    }
}
