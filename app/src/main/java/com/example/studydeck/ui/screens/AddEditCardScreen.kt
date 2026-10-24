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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
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
import com.example.studydeck.model.Flashcard

import com.example.studydeck.ui.theme.ItemState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: String?,
    cardState: ItemState<Flashcard>,
    onRetryLoad: () -> Unit,
    onSave: (question: String, answer: String, onError: (String) -> Unit) -> Unit,
    onCancel: () -> Unit
) {
    val isEditing = cardId != null

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEditing) "Edit Card" else "Add Card") })
        }
    ) { innerPadding ->
        if (!isEditing) {
            CardForm(existingCard = null, onSave = onSave, onCancel = onCancel, modifier = Modifier.padding(innerPadding))
        } else {
            // ignore a stale result that belongs to a different card
            val loaded = when (cardState) {
                is ItemState.Loaded -> cardState.item.takeIf { it.id == cardId }
                else -> null
            }
            when {
                loaded != null ->
                    CardForm(existingCard = loaded, onSave = onSave, onCancel = onCancel, modifier = Modifier.padding(innerPadding))

                cardState is ItemState.Error ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = cardState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRetryLoad) { Text("Try again") }
                        OutlinedButton(onClick = onCancel) { Text("Back") }
                    }

                else ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
            }
        }
    }
}

@Composable
private fun CardForm(
    existingCard: Flashcard?,
    onSave: (question: String, answer: String, onError: (String) -> Unit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var question by remember { mutableStateOf(existingCard?.question ?: "") }
    var answer by remember { mutableStateOf(existingCard?.answer ?: "") }

    var questionError by remember { mutableStateOf(false) }
    var answerError by remember { mutableStateOf(false) }
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
            value = question,
            onValueChange = { question = it; questionError = false },
            label = { Text("Question") },
            isError = questionError,
            supportingText = { if (questionError) Text("Question is required") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; answerError = false },
            label = { Text("Answer") },
            isError = answerError,
            supportingText = { if (answerError) Text("Answer is required") },
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
                    questionError = question.isBlank()
                    answerError = answer.isBlank()
                    if (!questionError && !answerError) {
                        isSaving = true
                        saveError = null
                        onSave(question.trim(), answer.trim()) { message ->
                            isSaving = false
                            saveError = message
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        }
    }
}
