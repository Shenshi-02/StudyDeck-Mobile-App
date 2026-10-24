package com.example.studydeck

import android.app.Application
import com.example.studydeck.data.FirebaseRepository

class StudyDeckApplication : Application() {
    val repository by lazy { FirebaseRepository() }
}
