package com.example.saferoute.data.repository

import com.example.saferoute.data.remote.FirebaseService
import com.example.saferoute.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val auth = FirebaseService.getInstance()
    val firestoreService =
        FirestoreService(com.google.firebase.firestore.FirebaseFirestore.getInstance())


    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    fun getUserProfile(userId: String, onResult: (Boolean, Map<String, Any>?, String?) -> Unit) {
        firestoreService.getUserData(userId, onResult)
    }


    fun updateProfile(newName: String, newEmail: String, onResult: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        if (userId == null) {
            onResult(false, "User not logged in")
            return
        }


        firestoreService.updateUserData(
            userId,
            newName,
            newEmail
        ) { isFirestoreSuccess, firestoreError ->
            if (isFirestoreSuccess) {

                currentUser.verifyBeforeUpdateEmail(newEmail)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            onResult(true, null)
                        } else {

                            onResult(true, "Profile updated. Verification email sent to $newEmail")
                        }
                    }
            } else {
                onResult(false, firestoreError)
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    fun logout() {
        auth.signOut()
    }
}