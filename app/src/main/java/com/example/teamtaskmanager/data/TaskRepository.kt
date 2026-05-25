package com.example.teamtaskmanager.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Dodanie zadania do Firestore
    suspend fun addTask(task: Task) {
        try {
            firestore.collection("tasks").document(task.id).set(task).await()
        } catch (e: Exception) {
            // Obsługa błędu
            throw e
        }
    }

    // Pobranie wszystkich zadań w formie Flow, aby można było subskrybować zmiany
    fun getAllTasks(): Flow<List<Task>> = callbackFlow {
        val snapshotListener = firestore.collection("tasks")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                // Przetwarzamy dokumenty
                val tasks = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Task::class.java)
                } ?: emptyList()

                trySend(tasks)
            }

        // Umożliwiamy zamknięcie listenera
        awaitClose { snapshotListener.remove() }
    }

    // Pobranie zadania po ID
    suspend fun getTaskById(taskId: String): Task? {
        return try {
            val document = firestore.collection("tasks").document(taskId).get().await()
            document.toObject(Task::class.java)
        } catch (e: Exception) {
            null // Możesz dodać logowanie błędu w produkcyjnej wersji
        }
    }

    // Zaktualizowanie statusu zadania
    suspend fun updateTaskStatus(taskId: String, newStatus: String) {
        try {
            val taskRef = firestore.collection("tasks").document(taskId)
            taskRef.update("status", newStatus).await()
        } catch (e: Exception) {
            // Obsługa błędu
            throw e
        }
    }

    suspend fun updateTask(task: Task) {
        try {
            firestore.collection("tasks").document(task.id).set(task).await()  // Zapisujemy zaktualizowane zadanie w Firestore
        } catch (e: Exception) {
            // Obsługa błędów, np. logowanie
            throw e
        }
    }

    suspend fun deleteTask(taskId: String) {
        firestore.collection("tasks").document(taskId).delete().await()
    }

    fun getAllTasksFlow(teamId: String): Flow<List<Task>> = flow {
        val docs = firestore.collection("tasks").whereEqualTo("teamId", teamId).get().await().documents
        emit(docs.mapNotNull { it.toObject(Task::class.java) })
    }
}
