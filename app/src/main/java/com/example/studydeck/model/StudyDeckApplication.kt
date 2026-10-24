package com.example.studydeck.model

import android.app.Application

class StudyDeckApplication : Application() {
    val repository by lazy { FirebaseRepository() }
}