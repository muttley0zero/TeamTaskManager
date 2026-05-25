package com.example.teamtaskmanager.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import javax.inject.Inject

class FirebaseService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val tasksRef = db.collection("tasks")

    fun getTasks(): Flow<List<Task>> = callbackFlow {
        val listener = tasksRef.addSnapshotListener { snapshot, _ ->
            val tasks = snapshot?.documents?.mapNotNull { it.toObject(Task::class.java)?.copy(id = it.id) } ?: emptyList()
            trySend(tasks)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addTask(task: Task) {
        tasksRef.add(task)
    }

    suspend fun updateTask(task: Task) {
        tasksRef.document(task.id).set(task)
    }

    suspend fun deleteTask(taskId: String) {
        tasksRef.document(taskId).delete()
    }
}
