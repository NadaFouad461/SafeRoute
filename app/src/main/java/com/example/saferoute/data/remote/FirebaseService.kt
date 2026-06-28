package com.example.saferoute.data.remote

import com.google.firebase.auth.FirebaseAuth

object FirebaseService {
    fun getInstance(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}