package com.example.teamtaskmanager.viewmodel

data class Team(
    val ownerId: String,
    val tasks: Map<String, Any> = emptyMap(),
    val subteams: Map<String, Any> = emptyMap(),
    val pendingUsers: List<String> = emptyList() // Lista użytkowników oczekujących na zatwierdzenie
)