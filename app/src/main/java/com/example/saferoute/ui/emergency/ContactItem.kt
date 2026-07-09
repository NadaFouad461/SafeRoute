package com.example.saferoute.models

data class ContactItem(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
    val isPriority: Boolean = false,
    val fcmToken: String = "",
    val userUid: String = ""
)