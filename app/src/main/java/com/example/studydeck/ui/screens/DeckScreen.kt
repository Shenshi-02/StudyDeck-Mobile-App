package com.example.studydeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.studydeck.model.Flashcard
import com.example.studydeck.ui.components.ConfirmDeleteDialog
import com.example.studydeck.ui.components.ErrorView
import com.example.studydeck.ui.components.LoadingView
import com.example.studydeck.ui.theme.FlashcardListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    deckTitle: String,
    uiState: FlashcardListState,
    actionError: String?,
    onActionErrorShown: () -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onCardClick: (String) -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onReviewClick: () -> Unit
) {
    var cardToDelete by remember { mutableStateOf<Flashcard?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbarHostState.showSnackbar(actionError)
            onActionErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(deckTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to My Decks")
                    }
                },
                actions = {
                    IconButton(onClick = onReviewClick, enabled = uiState.cards.isNotEmpty()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Study")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCardClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add flashcard")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingView()

                uiState.error != null -> ErrorView(uiState.error, onRetry)

                uiState.cards.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flashcards in this deck yet.\nTap + to add your first card!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.cards, key = { it.id }) { card ->
                            FlashcardRow(
                                card = card,
                                onClick = { onCardClick(card.id) },
                                onDelete = { cardToDelete = card }
                            )
                        }
                    }
                }
            }
        }
    }

    cardToDelete?.let { card ->
        ConfirmDeleteDialog(
            title = "Delete flashcard?",
            message = "This card will be removed permanently.",
            onConfirm = {
                onDeleteCard(card)
                cardToDelete = null
            },
            onDismiss = { cardToDelete = null }
        )
    }
}

@Composable
private fun FlashcardRow(card: Flashcard, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete card",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
