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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
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
import com.example.studydeck.model.Deck
import com.example.studydeck.ui.components.ConfirmDeleteDialog
import com.example.studydeck.ui.components.ErrorView
import com.example.studydeck.ui.components.LoadingView
import com.example.studydeck.ui.theme.DeckListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    uiState: DeckListState,
    actionError: String?,
    onActionErrorShown: () -> Unit,
    onRetry: () -> Unit,
    onAddDeckClick: () -> Unit,
    onDeckClick: (String) -> Unit,
    onEditDeck: (String) -> Unit,
    onDeleteDeck: (Deck) -> Unit,
    onLogoutClick: () -> Unit
) {
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
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
                title = { Text("My Decks") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDeckClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add deck")
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

                else -> {
                    Text(
                        text = "Hi, $userName! You have ${uiState.decks.size} deck(s).",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )

                    if (uiState.decks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No decks yet.\nTap + to create your first deck!",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.decks, key = { it.id }) { deck ->
                                DeckRow(
                                    deck = deck,
                                    onClick = { onDeckClick(deck.id) },
                                    onEdit = { onEditDeck(deck.id) },
                                    onDelete = { deckToDelete = deck }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deckToDelete?.let { deck ->
        ConfirmDeleteDialog(
            title = "Delete deck?",
            message = "\"${deck.title}\" and all of its flashcards will be removed permanently.",
            onConfirm = {
                onDeleteDeck(deck)
                deckToDelete = null
            },
            onDismiss = { deckToDelete = null }
        )
    }
}

@Composable
private fun DeckRow(deck: Deck, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AssistChip(onClick = {}, label = { Text(deck.subject) })
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (deck.description.isNotBlank()) {
                    Text(
                        text = deck.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit deck")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete deck",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
