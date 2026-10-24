package com.example.studydeck.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studydeck.ui.screens.AddEditCardScreen
import com.example.studydeck.ui.screens.AddEditDeckScreen
import com.example.studydeck.ui.screens.DeckScreen
import com.example.studydeck.ui.screens.HomeScreen
import com.example.studydeck.ui.screens.LoginScreen
import com.example.studydeck.ui.screens.RegisterScreen
import com.example.studydeck.ui.screens.ReviewScreen
import com.example.studydeck.model.AuthViewModel
import com.example.studydeck.model.DeckViewModel
import com.example.studydeck.ui.theme.FlashcardListState
import com.example.studydeck.model.FlashcardViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")


    object Deck : Screen("deck/{deckId}") {
        fun createRoute(deckId: String) = "deck/$deckId"
    }

    object AddEditDeck : Screen("add_edit_deck?deckId={deckId}") {
        fun createRoute(deckId: String? = null) =
            if (deckId == null) "add_edit_deck" else "add_edit_deck?deckId=$deckId"
    }

    object AddEditCard : Screen("add_edit_card/{deckId}?cardId={cardId}") {
        fun createRoute(deckId: String, cardId: String? = null) =
            if (cardId == null) "add_edit_card/$deckId" else "add_edit_card/$deckId?cardId=$cardId"
    }

    object Review : Screen("review/{deckId}") {
        fun createRoute(deckId: String) = "review/$deckId"
    }
}

@Composable
fun StudyDeckNavGraph(
    authViewModel: AuthViewModel,
    deckViewModel: DeckViewModel,
    flashcardViewModel: FlashcardViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val deckList by deckViewModel.uiState.collectAsStateWithLifecycle()
    val deckActionError by deckViewModel.actionError.collectAsStateWithLifecycle()
    val cardList by flashcardViewModel.uiState.collectAsStateWithLifecycle()
    val cardActionError by flashcardViewModel.actionError.collectAsStateWithLifecycle()

    val startDestination = remember {
        if (authViewModel.currentUser.value != null) Screen.Home.route else Screen.Login.route
    }

    LaunchedEffect(currentUser?.id) {
        if (currentUser != null) {
            deckViewModel.loadDecks()
        } else {
            deckViewModel.clearDecks()
            flashcardViewModel.clearCards()
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(currentUser, currentRoute) {
        val onAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route
        if (currentUser == null && currentRoute != null && !onAuthScreen) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = { email, password, onError ->
                    authViewModel.login(
                        email = email,
                        password = password,
                        onSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onError = onError
                    )
                },
                onGoToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegister = { fullName, email, password, onError ->
                    authViewModel.register(
                        name = fullName,
                        email = email,
                        password = password,
                        onSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onError = onError
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }


        composable(Screen.Home.route) {
            HomeScreen(
                userName = currentUser?.displayName ?: "",
                uiState = deckList,
                actionError = deckActionError,
                onActionErrorShown = { deckViewModel.dismissActionError() },
                onRetry = { deckViewModel.loadDecks() },
                onAddDeckClick = { navController.navigate(Screen.AddEditDeck.createRoute()) },
                onDeckClick = { deckId -> navController.navigate(Screen.Deck.createRoute(deckId)) },
                onEditDeck = { deckId -> navController.navigate(Screen.AddEditDeck.createRoute(deckId)) },
                onDeleteDeck = { deck -> deckViewModel.deleteDeck(deck) },
                onLogoutClick = { authViewModel.logout() }
            )
        }

        composable(
            route = Screen.AddEditDeck.route,
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            val deckState by deckViewModel.deckState.collectAsStateWithLifecycle()

            // Edit mode: fetch this one deck from Firestore by its document ID.
            LaunchedEffect(deckId) {
                if (deckId != null) deckViewModel.loadDeck(deckId) else deckViewModel.clearDeck()
            }

            AddEditDeckScreen(
                deckId = deckId,
                deckState = deckState,
                onRetryLoad = { if (deckId != null) deckViewModel.loadDeck(deckId) },
                onSave = { title, subject, description, onError ->
                    // Go back only after Firestore confirmed the save.
                    val onSuccess = { navController.popBackStack(); Unit }
                    if (deckId != null) {
                        deckViewModel.updateDeck(deckId, title, subject, description, onSuccess, onError)
                    } else {
                        deckViewModel.createDeck(title, subject, description, onSuccess, onError)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Deck.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId") ?: return@composable

            LaunchedEffect(deckId) { flashcardViewModel.loadCards(deckId) }

            // do not show the cards of a previously opened deck
            val state = if (cardList.deckId == deckId) cardList else FlashcardListState(isLoading = true)
            val deck = deckList.decks.find { it.id == deckId }

            DeckScreen(
                deckTitle = deck?.title ?: "",
                uiState = state,
                actionError = cardActionError,
                onActionErrorShown = { flashcardViewModel.dismissActionError() },
                onRetry = { flashcardViewModel.loadCards(deckId) },
                onBackClick = { navController.popBackStack() },
                onAddCardClick = { navController.navigate(Screen.AddEditCard.createRoute(deckId)) },
                onCardClick = { cardId ->
                    navController.navigate(Screen.AddEditCard.createRoute(deckId, cardId))
                },
                onDeleteCard = { card -> flashcardViewModel.deleteCard(deckId, card) },
                onReviewClick = { navController.navigate(Screen.Review.createRoute(deckId)) }
            )
        }

        composable(
            route = Screen.AddEditCard.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("cardId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId") ?: return@composable
            val cardId = backStackEntry.arguments?.getString("cardId")
            val cardState by flashcardViewModel.cardState.collectAsStateWithLifecycle()

            // Edit mode: fetch this one card from Firestore by its document ID.
            LaunchedEffect(cardId) {
                if (cardId != null) flashcardViewModel.loadCard(deckId, cardId) else flashcardViewModel.clearCard()
            }

            AddEditCardScreen(
                cardId = cardId,
                cardState = cardState,
                onRetryLoad = { if (cardId != null) flashcardViewModel.loadCard(deckId, cardId) },
                onSave = { question, answer, onError ->
                    // Go back only after Firestore confirmed the save.
                    val onSuccess = { navController.popBackStack(); Unit }
                    if (cardId != null) {
                        flashcardViewModel.updateCard(deckId, cardId, question, answer, onSuccess, onError)
                    } else {
                        flashcardViewModel.addCard(deckId, question, answer, onSuccess, onError)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Review.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId") ?: return@composable
            val state = if (cardList.deckId == deckId) cardList else FlashcardListState(isLoading = true)
            val deck = deckList.decks.find { it.id == deckId }

            ReviewScreen(
                flashcards = state.cards,
                subject = deck?.subject ?: "",
                onBackClick = { navController.popBackStack() },
                onSessionEnd = { cardsViewed, startedAt ->
                    flashcardViewModel.recordStudySession(
                        deckId = deckId,
                        deckTitle = deck?.title ?: "",
                        cardsReviewed = cardsViewed,
                        totalCards = state.cards.size,
                        startedAtMillis = startedAt
                    )
                }
            )
        }
    }
}
