package com.example.studydeck.data

import com.example.studydeck.model.Deck
import com.example.studydeck.model.Flashcard
import com.example.studydeck.model.User
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun decksRef(uid: String) = userDoc(uid).collection("decks")

    private fun cardsRef(uid: String, deckId: String) =
        decksRef(uid).document(deckId).collection("cards")



    // Firebase keeps the session, so this is not null if the user never logged out.
    // The real name is loaded from Firestore with loadDisplayName().
    fun currentUser(): User? {
        val user = auth.currentUser ?: return null
        val email = user.email ?: ""
        return User(user.uid, email.substringBefore("@"), email)
    }

    fun register(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                val user = auth.currentUser
                if (!task.isSuccessful || user == null) {
                    onResult(false, authMessage(task.exception))
                    return@addOnCompleteListener
                }

                val profile = hashMapOf(
                    "displayName" to name.trim(),
                    "email" to email.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )

                userDoc(user.uid)
                    .set(profile)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { error ->
                        // profile was not saved, so remove the new account
                        user.delete()
                        onResult(false, dbMessage(error))
                    }
            }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, authMessage(task.exception))
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    // Reads the name saved in users/{uid}. Gives null if it can't be loaded.
    fun loadDisplayName(onResult: (String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null)
            return
        }
        userDoc(uid).get()
            .addOnSuccessListener { document -> onResult(document.getString("displayName")) }
            .addOnFailureListener { onResult(null) }
    }




    fun listenToDecks(onResult: (List<Deck>, String?) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(emptyList(), "Please log in first")
            return null
        }

        return decksRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), dbMessage(error))
                    return@addSnapshotListener
                }
                val decks = snapshot?.documents?.mapNotNull { it.toDeck() } ?: emptyList()
                onResult(decks, null)
            }
    }


    fun getDeck(deckId: String, onResult: (Deck?, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null, "Please log in first")
            return
        }

        decksRef(uid).document(deckId).get()
            .addOnSuccessListener { document ->
                val deck = document.toDeck()
                if (deck != null) onResult(deck, null) else onResult(null, "Deck not found")
            }
            .addOnFailureListener { error -> onResult(null, dbMessage(error)) }
    }

    fun createDeck(
        title: String,
        subject: String,
        description: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please log in first")
            return
        }

        val deck = hashMapOf(
            "title" to title.trim(),
            "subject" to subject.trim(),
            "description" to description.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        decksRef(uid)
            .add(deck)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
    }

    fun updateDeck(
        deckId: String,
        title: String,
        subject: String,
        description: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please log in first")
            return
        }

        val updates = mapOf(
            "title" to title.trim(),
            "subject" to subject.trim(),
            "description" to description.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        decksRef(uid).document(deckId)
            .update(updates)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
    }


    fun deleteDeck(deckId: String, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please log in first")
            return
        }

        val deckRef = decksRef(uid).document(deckId)
        deckRef.collection("cards").get()
            .addOnSuccessListener { cards ->
                val batch = db.batch()
                cards.documents.forEach { batch.delete(it.reference) }
                batch.delete(deckRef)
                batch.commit()
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
            }
            .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
    }



    fun listenToCards(deckId: String, onResult: (List<Flashcard>, String?) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(emptyList(), "Please log in first")
            return null
        }

        return cardsRef(uid, deckId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), dbMessage(error))
                    return@addSnapshotListener
                }
                val cards = snapshot?.documents?.mapNotNull { it.toCard() } ?: emptyList()
                onResult(cards, null)
            }
    }


    fun getCard(deckId: String, cardId: String, onResult: (Flashcard?, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null, "Please log in first")
            return
        }

        cardsRef(uid, deckId).document(cardId).get()
            .addOnSuccessListener { document ->
                val card = document.toCard()
                if (card != null) onResult(card, null) else onResult(null, "Flashcard not found")
            }
            .addOnFailureListener { error -> onResult(null, dbMessage(error)) }
    }

    fun addCard(
        deckId: String,
        question: String,
        answer: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please log in first")
            return
        }

        val card = hashMapOf(
            "question" to question.trim(),
            "answer" to answer.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        cardsRef(uid, deckId)
            .add(card)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
    }

    fun updateCard(
        deckId: String,
        cardId: String,
        question: String,
        answer: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please log in first")
            return
        }

        val updates = mapOf(
            "question" to question.trim(),
            "answer" to answer.trim()
        )

        cardsRef(uid, deckId).document(cardId)
            .update(updates)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
    }

    fun deleteCard(deckId: String, cardId: String, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please log in first")
            return
        }

        cardsRef(uid, deckId).document(cardId)
            .delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, dbMessage(error)) }
    }



    private fun DocumentSnapshot.toDeck(): Deck? {
        if (!exists()) return null
        return Deck(
            id = id,
            title = getString("title") ?: return null,
            subject = getString("subject") ?: "General",
            description = getString("description") ?: ""
        )
    }

    private fun DocumentSnapshot.toCard(): Flashcard? {
        if (!exists()) return null
        return Flashcard(
            id = id,
            question = getString("question") ?: return null,
            answer = getString("answer") ?: return null
        )
    }

    private fun authMessage(error: Exception?): String = when (error) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException -> "Password is too weak"
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseNetworkException -> "No internet connection"
        else -> error?.localizedMessage ?: "Something went wrong"
    }

    private fun dbMessage(error: Exception): String {
        val denied = error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        return if (denied) {
            "Permission denied. Check your Firestore rules."
        } else {
            error.localizedMessage ?: "Something went wrong"
        }
    }
}
