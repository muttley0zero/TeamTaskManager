package com.example.teamtaskmanager.viewmodel

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamtaskmanager.MainActivity
import com.example.teamtaskmanager.data.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var taskListener: ListenerRegistration? = null
    private var currentLoadedUid: String? = null
    private var currentLoadedTeamId: String? = null

    fun loadTasks(teamId: String, currentUserId: String) {
        // Jeśli już słuchamy dla tego samego użytkownika i zespołu, nie restartuj
        if (currentLoadedUid == currentUserId && currentLoadedTeamId == teamId) return
        
        Log.d("TaskViewModel", "Inicjalizacja loadTasks dla UID: $currentUserId, Team: $teamId")
        taskListener?.remove()
        currentLoadedUid = currentUserId
        currentLoadedTeamId = teamId

        val sharedPrefs = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        
        // Jeśli brak wpisu, ustawiamy na 24h wstecz
        if (!sharedPrefs.contains("last_check_$currentUserId")) {
            val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            sharedPrefs.edit().putLong("last_check_$currentUserId", yesterday).apply()
            Log.d("TaskViewModel", "Pierwsze logowanie - ustawiono lastCheck na 24h wstecz")
        }

        taskListener = firestore.collection("tasks")
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    _errorMessage.value = err.message
                    Log.e("TaskViewModel", "Błąd Firestore: ${err.message}")
                    return@addSnapshotListener
                }

                if (snap != null) {
                    val list = snap.documents.mapNotNull { it.toObject(Task::class.java) }
                    val lastCheck = sharedPrefs.getLong("last_check_$currentUserId", 0L)
                    var latestInBatch = lastCheck

                    Log.d("TaskViewModel", "Otrzymano snapshot. Rozmiar: ${list.size}, LastCheck: $lastCheck")

                    for (dc in snap.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val newTask = dc.document.toObject(Task::class.java)
                            
                            Log.d("TaskViewModel", "Nowe zadanie w systemie: ${newTask.title}, CreatedAt: ${newTask.createdAt}")

                            // Warunki powiadomienia
                            if (newTask.assignedToUserId == currentUserId && 
                                newTask.createdBy != currentUserId &&
                                newTask.createdAt > lastCheck) {
                                
                                Log.d("TaskViewModel", "Wysyłanie powiadomienia dla zadania: ${newTask.title}")
                                sendLocalNotification(newTask)
                            }
                            
                            if (newTask.createdAt > latestInBatch) {
                                latestInBatch = newTask.createdAt
                            }
                        }
                    }
                    
                    if (latestInBatch > lastCheck) {
                        sharedPrefs.edit().putLong("last_check_$currentUserId", latestInBatch).apply()
                        Log.d("TaskViewModel", "Zaktualizowano lastCheck na: $latestInBatch")
                    }
                    
                    _tasks.value = list
                }
            }
    }

    private fun sendLocalNotification(task: Task) {
        // Sprawdź uprawnienia (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("TaskViewModel", "Brak uprawnień do powiadomień!")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "task_channel_v2")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nowe zadanie: ${task.title}")
            .setContentText(task.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(task.id.hashCode(), notification)
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks").document(task.id).set(task)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks").document(task.id).set(task)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks").document(taskId).update("status", newStatus)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("tasks").document(taskId).delete()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> = flow {
        try {
            val snapshot = firestore.collection("tasks").document(taskId).get().await()
            emit(snapshot.toObject(Task::class.java))
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun clearError() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        taskListener?.remove()
        currentLoadedUid = null
        currentLoadedTeamId = null
    }
}
