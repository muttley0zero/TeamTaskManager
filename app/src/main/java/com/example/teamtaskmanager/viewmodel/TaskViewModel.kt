package com.example.teamtaskmanager.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamtaskmanager.data.Task
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadTasks(teamId: String) {
        firestore.collection("tasks")
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    _errorMessage.value = err.message
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Task::class.java) }
                    ?: emptyList()
                _tasks.value = list
            }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks")
                    .document(task.id)
                    .set(task)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks")
                    .document(task.id)
                    .set(task)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks")
                    .document(taskId)
                    .update("status", newStatus)
                _tasks.value = _tasks.value.map {
                    if (it.id == taskId) it.copy(status = newStatus) else it
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks")
                    .document(taskId)
                    .delete()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> = flow {
        try {
            val snapshot = firestore.collection("tasks")
                .document(taskId)
                .get()
                .await()
            emit(snapshot.toObject(Task::class.java))
        } catch (e: Exception) {
            Log.e("TaskViewModel", "getTaskById error: ", e)
            emit(null)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
