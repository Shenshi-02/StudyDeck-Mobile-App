package com.example.studydeck.model


data class Deck(
    val id: String = "",
    val title: String,
    val subject: String,
    val description: String,
    // id of the document in users/{uid}/subjects (empty if the deck has no subject)
    val subjectId: String = ""
)
