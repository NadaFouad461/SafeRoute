package com.example.saferoute.ui.auth

import androidx.lifecycle.ViewModel
import com.example.saferoute.data.repository.AuthRepository

class AuthViewModel: ViewModel() {
    private val authRepository = AuthRepository()


    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }


    fun getUserProfile(userId: String, onResult: (Boolean, Map<String, Any>?, String?) -> Unit) {
        authRepository.getUserProfile(userId, onResult)
    }


    fun updateProfile(name: String, email: String, onResult: (Boolean, String?) -> Unit) {
        authRepository.updateProfile(name, email, onResult)
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        authRepository.login(email, password, onResult)
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        authRepository.signUp(email, password, onResult)
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        authRepository.resetPassword(email, onResult)
    }

    fun logout() {
        authRepository.logout()
    }
}