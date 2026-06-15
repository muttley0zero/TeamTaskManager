package com.example.teamtaskmanager.screens

import android.os.Build
import android.widget.CalendarView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.data.Task
import com.example.teamtaskmanager.viewmodel.TaskViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCalendarScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    val user by authViewModel.appUser.collectAsState()
    if (user == null) return

    val role = user!!.role.lowercase()
    val uid = FirebaseAuth.getInstance().currentUser!!.uid
    val teamId = user!!.teamId

    // Load tasks - reagujemy na zmianę teamId i uid
    LaunchedEffect(teamId, uid) {
        if (teamId.isNotBlank()) taskViewModel.loadTasks(teamId, uid)
    }
    val tasks by taskViewModel.tasks.collectAsState()

    // Group tasks by date
    val tasksByDate: Map<LocalDate, List<Task>> = remember(tasks) {
        tasks.filter { t ->
            when (role) {
                "boss"  -> true
                "lead"  -> t.assignedToUserId == uid || t.assignedRole == "worker"
                else    -> t.assignedToUserId == uid
            }
        }.mapNotNull { t ->
            if (t.dueDate > 0) {
                val date = Instant.ofEpochMilli(t.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                date to t
            } else null
        }.groupBy({ it.first }, { it.second })
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val selectedTasks = tasksByDate[selectedDate] ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kalendarz zadań") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                AndroidView(
                    factory = { ctx ->
                        CalendarView(ctx).apply {
                            setOnDateChangeListener { _, year, month, dayOfMonth ->
                                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Zadania na $selectedDate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp)
            )

            if (selectedTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak zadań", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedTasks) { task ->
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Tytuł: ${task.title}", style = MaterialTheme.typography.bodyLarge)
                                Text("Opis: ${task.description}", style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
