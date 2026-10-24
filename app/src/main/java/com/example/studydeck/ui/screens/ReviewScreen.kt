package com.example.studydeck.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studydeck.model.Flashcard
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(
    flashcards: List<Flashcard>,
    subject: String,
    onBackClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { flashcards.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Mode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to My Flashcards")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Add a flashcard first to start studying.")
            }
        } else {
            val currentPage = pagerState.currentPage

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Card ${currentPage + 1} of ${flashcards.size}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    pageSpacing = 16.dp
                ) { page ->
                    FlipCard(card = flashcards[page], subject = subject)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } },
                        enabled = currentPage > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("< Previous")
                    }
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } },
                        enabled = currentPage < flashcards.size - 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next >")
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(card: Flashcard, subject: String) {
    var showAnswer by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showAnswer) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )
    val isBackVisible = rotation > 90f

    Card(
        onClick = { showAnswer = !showAnswer },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                // the back side would be mirrored, so flip it back
                .graphicsLayer { rotationY = if (isBackVisible) 180f else 0f },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (subject.isNotBlank()) {
                AssistChip(onClick = {}, label = { Text(subject) })
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = if (isBackVisible) "ANSWER" else "QUESTION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isBackVisible) card.answer else card.question,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isBackVisible) "(tap to see question)" else "(tap card to flip)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
