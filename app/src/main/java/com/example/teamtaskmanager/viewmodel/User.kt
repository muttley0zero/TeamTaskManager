package com.example.teamtaskmanager.viewmodel

data class User(
    val email: String = "",
    val role: String = "",
    val teamId: String = "",
    val status: String = "pending", // "pending" lub "approved"
    val fcmToken: String? = null
)