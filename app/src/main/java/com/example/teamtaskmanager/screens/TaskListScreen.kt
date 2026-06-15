package com.example.teamtaskmanager.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.data.Task
import com.example.teamtaskmanager.viewmodel.TaskViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    val user by authViewModel.appUser.collectAsState()
    if (user == null) return

    val teamId = user!!.teamId
    val role = user!!.role.lowercase()
    val uid = FirebaseAuth.getInstance().currentUser!!.uid

    LaunchedEffect(teamId) {
        if (teamId.isNotBlank()) taskViewModel.loadTasks(teamId, uid)
    }

    val tasks by taskViewModel.tasks.collectAsState()
    val err by taskViewModel.errorMessage.collectAsState()

    val pendingTasks = tasks.filter { it.status == "pending" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            authViewModel.logout()
                            navController.navigate("login") { popUpTo(0) }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Wyloguj się")
                        }
                        Spacer(Modifier.width(8.dp))
                        
                        val clipboardManager = LocalClipboardManager.current
                        val context = LocalContext.current

                        Column {
                            Text("Zadania zespołu")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (teamId.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(teamId))
                                        Toast.makeText(context, "ID zespołu skopiowane!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Team ID: $teamId", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopiuj ID",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("task_calendar") }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Kalendarz zadań")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                err != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Błąd: $err", color = MaterialTheme.colorScheme.error)
                }
                tasks.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak zadań")
                }
                else -> when (role) {
                    "boss" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            listOf("worker", "lead").forEach { roleName ->
                                item { SectionHeader(roleName) }
                                items(tasks.filter { it.assignedRole == roleName }) { task ->
                                    TaskCard(
                                        task = task,
                                        isAssignedToUser = task.assignedToUserId == uid,
                                        isUserBossOrLead = true,
                                        onEditTask = { navController.navigate("edit_task/${task.id}") },
                                        onDeleteTask = { taskViewModel.deleteTask(task.id) },
                                        onTaskClick = { navController.navigate("task_details/${task.id}") },
                                        onMarkAsCompleted = {
                                            if (task.assignedToUserId == uid && task.status != "completed")
                                                taskViewModel.updateTaskStatus(task.id, "completed")
                                        }
                                    )
                                }
                            }
                        }
                    }
                    "lead" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Sekcja: zadania pracowników
                            item { SectionHeader("Zadania pracowników") }
                            items(tasks.filter { it.assignedRole == "worker" }) { task ->
                                TaskCard(
                                    task = task,
                                    isAssignedToUser = task.assignedToUserId == uid,
                                    isUserBossOrLead = true,
                                    onEditTask = { navController.navigate("edit_task/${task.id}") },
                                    onDeleteTask = { taskViewModel.deleteTask(task.id) },
                                    onTaskClick = { navController.navigate("task_details/${task.id}") },
                                    onMarkAsCompleted = {
                                        if (task.assignedToUserId == uid)
                                            taskViewModel.updateTaskStatus(task.id, "completed")
                                    }
                                )
                            }
                            // Sekcja: zadania przypisane leadowi
                            item { SectionHeader("Moje zadania") }
                            items(pendingTasks.filter { it.assignedToUserId == uid }) { task ->
                                TaskCard(
                                    task = task,
                                    isAssignedToUser = true,
                                    isUserBossOrLead = true,
                                    onEditTask = { navController.navigate("edit_task/${task.id}") },
                                    onDeleteTask = { taskViewModel.deleteTask(task.id) },
                                    onTaskClick = { navController.navigate("task_details/${task.id}") },
                                    onMarkAsCompleted = { taskViewModel.updateTaskStatus(task.id, "completed") }
                                )
                            }
                        }
                    }
                    "worker", "guest" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(pendingTasks.filter { it.assignedToUserId == uid }) { task ->
                                TaskCard(
                                    task = task,
                                    isAssignedToUser = true,
                                    isUserBossOrLead = false,
                                    onEditTask = { navController.navigate("edit_task/${task.id}") },
                                    onDeleteTask = { taskViewModel.deleteTask(task.id) },
                                    onTaskClick = { navController.navigate("task_details/${task.id}") },
                                    onMarkAsCompleted = { taskViewModel.updateTaskStatus(task.id, "completed") }
                                )
                            }
                        }
                    }
                }
            }
            // FABs...
            if (role in listOf("boss", "lead")) FloatingActionButton(
                onClick = { navController.navigate("add_task") }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Dodaj zadanie") }

            if (role == "boss") FloatingActionButton(
                onClick = { navController.navigate("approve_user") }, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) { Icon(Icons.Filled.Person, contentDescription = "Zatwierdź użytkowników") }

            if (role in listOf("lead", "worker", "guest")) FloatingActionButton(
                onClick = { navController.navigate("team_members") }, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) { Icon(Icons.Filled.Person, contentDescription = "Członkowie zespołu") }
        }
    }
}

@Composable
fun SectionHeader(roleName: String) {
    Text(
        text = roleName,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun TaskCard(
    task: Task,
    isAssignedToUser: Boolean,
    isUserBossOrLead: Boolean,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onTaskClick: () -> Unit,
    onMarkAsCompleted: () -> Unit
) {
    val status = task.status ?: "Brak statusu"
    val cardBackgroundColor = when {
        status == "completed" -> Color.Green.copy(alpha = 0.2f)
        task.dueDate != null && task.dueDate < System.currentTimeMillis() -> Color.Red.copy(alpha = 0.2f)
        status == "pending" -> Color.Gray.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onTaskClick).clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tytuł: ${task.title}")
            Text("Opis: ${task.description}")
            Text("Priorytet: ${task.priority}")
            Text("Kategoria: ${task.category}")
            Text("Status: $status", style = MaterialTheme.typography.bodyMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status != "completed") {
                    if (isUserBossOrLead) {
                        IconButton(onClick = onEditTask) { Icon(Icons.Filled.Edit, contentDescription = "Edytuj zadanie") }
                        IconButton(onClick = onDeleteTask) { Icon(Icons.Filled.Delete, contentDescription = "Usuń zadanie") }
                    }
                    if (isAssignedToUser) Button(onClick = onMarkAsCompleted) { Text("Zakończ zadanie") }
                } else if (isUserBossOrLead) {
                    IconButton(onClick = onDeleteTask) { Icon(Icons.Filled.Delete, contentDescription = "Usuń zadanie") }
                }
            }
        }
    }
}