package com.example.studydeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studydeck.model.Deck
import com.example.studydeck.ui.components.LoadingView
import com.example.studydeck.ui.theme.ItemState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeckScreen(
    deckId: String?,
    deckState: ItemState<Deck>,
    onRetryLoad: () -> Unit,
    onSave: (title: String, subject: String, description: String, onError: (String) -> Unit) -> Unit,
    onCancel: () -> Unit
) {
    val isEditing = deckId != null

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEditing) "Edit Deck" else "New Deck") })
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        if (!isEditing) {
            DeckForm(existingDeck = null, onSave = onSave, onCancel = onCancel, modifier = modifier)
        } else {
            // ignore a stale result that belongs to a different deck
            val loaded = when (deckState) {
                is ItemState.Loaded -> deckState.item.takeIf { it.id == deckId }
                else -> null
            }
            when {
                loaded != null ->
                    DeckForm(existingDeck = loaded, onSave = onSave, onCancel = onCancel, modifier = modifier)

                deckState is ItemState.Error ->
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = deckState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRetryLoad) { Text("Try again") }
                        OutlinedButton(onClick = onCancel) { Text("Back") }
                    }

                else -> LoadingView()
            }
        }
    }
}

@Composable
private fun DeckForm(
    existingDeck: Deck?,
    onSave: (title: String, subject: String, description: String, onError: (String) -> Unit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(existingDeck?.title ?: "") }
    var subject by remember { mutableStateOf(existingDeck?.subject ?: "") }
    var description by remember { mutableStateOf(existingDeck?.description ?: "") }

    var titleError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; titleError = false },
            label = { Text("Title") },
            singleLine = true,
            isError = titleError,
            supportingText = { if (titleError) Text("Title is required") },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        saveError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    titleError = title.isBlank()
                    if (!titleError) {
                        isSaving = true
                        saveError = null
                        onSave(title.trim(), subject.trim().ifBlank { "General" }, description.trim()) { message ->
                            isSaving = false
                            saveError = message
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        }
    }
}
