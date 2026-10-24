# StudyDeck

Flashcard app for students (Kotlin, Jetpack Compose, Firebase Authentication + Cloud Firestore).

## Features
- Sign up / log in (each student has their own account)
- Passwords must be at least 8 characters with letters and numbers (handled by Firebase Authentication)
- Create, read, update, and delete decks (title, subject, description)
- Create, read, update, and delete flashcards inside each deck
- Data is saved in Cloud Firestore: `users/{uid}/decks/{deckId}/cards/{cardId}`
- Study mode per deck: swipe or use Previous/Next, tap the card to flip it

## Project structure
```
com.example.studydeck
|-- StudyDeckApplication.kt   creates the FirebaseRepository
|-- MainActivity.kt
|-- model/                    User, Deck, Flashcard (plain data classes)
|-- data/                     FirebaseRepository (Authentication + Firestore)
|-- viewmodel/                AuthViewModel, DeckViewModel, FlashcardViewModel, StudyDeckFactory
|-- navigation/               NavGraph
|-- ui/screens/               Login, Register, Home (decks), Deck (cards), AddEditDeck, AddEditCard, Review
|-- ui/components/            PasswordField, CommonViews
|-- ui/theme/                 Color, Type, Theme
`-- util/                     InputValidator
```

## Firebase setup (required)
1. In the Firebase console create/select a project and add an Android app with package `com.example.studydeck`.
2. Download `google-services.json` and put it in the `app/` folder.
3. Build > Authentication > Sign-in method: enable **Email/Password**.
4. Build > Firestore Database: create the database, then paste `firestore.rules` into the Rules tab and Publish.

## Run
Open the `StudyDeck` folder in Android Studio, let Gradle sync, then run on an emulator or device (minSdk 24).
