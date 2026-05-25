package com.example.teamtaskmanager.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.teamtaskmanager.data.Task
import com.example.teamtaskmanager.viewmodel.TaskViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskDetailScreen(
    taskId: String,
    navController: NavController,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    // Załaduj szczegóły zadania
    val task by taskViewModel.getTaskById(taskId).collectAsState(initial = null)

    // Jeżeli zadanie jeszcze nie załadowane, pokaż ładowanie
    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Formatuj daty, jeśli chcesz je wyświetlać w formacie
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Jeśli startDate i dueDate są typu String, parsujemy je na LocalDate
    val startDate = task!!.startDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    val dueDate = task!!.dueDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły zadania") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wyświetl detale zadania
            Text("Tytuł: ${task!!.title}")
            Text("Opis: ${task!!.description}")
            Text("Priorytet: ${task!!.priority}")
            Text("Kategoria: ${task!!.category}")
            Text("Data rozpoczęcia: ${startDate?.format(dateFormatter)}")
            Text("Data zakończenia: ${dueDate?.format(dateFormatter)}")

            // Jeśli użytkownik jest przełożonym, pozwól na edycję
            if (task!!.assignedToUserId == FirebaseAuth.getInstance().currentUser?.uid) {
                // Jeśli przełożony, dodaj możliwość edycji i usuwania zadania
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { navController.navigate("edit_task/${task!!.id.toString()}") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edytuj zadanie")
                    }
                    IconButton(onClick = { taskViewModel.deleteTask(task!!.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Usuń zadanie")
                    }
                }
            }
        }
    }
}
