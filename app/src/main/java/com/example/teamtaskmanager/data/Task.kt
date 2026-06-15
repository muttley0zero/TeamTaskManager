package com.example.teamtaskmanager.data

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedToUserId: String = "",
    val assignedRole: String = "",
    val createdBy: String = "",
    val teamId: String = "",
    val status: String = "open",
    val priority: String = "medium",  // Priorytet
    val category: String = "",        // Kategoria
    val dueDate: Long = 0L,
    val startDate: Long = 0L,
    val createdAt: Long = 0L
)
