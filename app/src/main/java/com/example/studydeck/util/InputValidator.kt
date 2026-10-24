package com.example.studydeck.util

import android.util.Patterns

object InputValidator {
    const val MIN_PASSWORD_LENGTH = 8

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Full name is required"
        name.trim().length < 2 -> "Name is too short"
        else -> null
    }

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email is required"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isEmpty() -> "Password is required"
        password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
        password.none { it.isLetter() } -> "Password must contain at least one letter"
        password.none { it.isDigit() } -> "Password must contain at least one number"
        else -> null
    }
}
