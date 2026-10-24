package com.example.studydeck.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow(repository.currentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // user is still logged in from the last time the app was opened
        if (_currentUser.value != null) loadName()
    }

    fun register(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.register(name, email, password) { success, message ->
            if (success) {
                _currentUser.value = repository.currentUser()?.copy(displayName = name.trim())
                onSuccess()
            } else {
                onError(message ?: "Registration failed")
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.login(email, password) { success, message ->
            if (success) {
                _currentUser.value = repository.currentUser()
                onSuccess()
                loadName()
            } else {
                onError(message ?: "Login failed")
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
    }


    private fun loadName() {
        val user = repository.currentUser() ?: return
        repository.loadDisplayName { name ->
            val stillLoggedIn = repository.currentUser()?.id == user.id
            if (name != null && stillLoggedIn) {
                _currentUser.value = user.copy(displayName = name)
            }
        }
    }
}