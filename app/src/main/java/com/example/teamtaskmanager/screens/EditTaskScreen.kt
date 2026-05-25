package com.example.teamtaskmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.teamtaskmanager.viewmodel.TaskViewModel
import com.example.teamtaskmanager.data.Task
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditTaskScreen(
    navController: NavHostController,
    taskId: String,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    var task by remember { mutableStateOf<Task?>(null) }

    // Pobierz dane zadania przy starcie
    LaunchedEffect(taskId) {
        taskViewModel.getTaskById(taskId).collectLatest { fetchedTask ->
            task = fetchedTask
        }
    }

    task?.let { original ->
        var title by remember { mutableStateOf(original.title) }
        var description by remember { mutableStateOf(original.description) }
        var priority by remember { mutableStateOf(original.priority) }
        var category by remember { mutableStateOf(original.category) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytuł") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = priority,
                onValueChange = { priority = it },
                label = { Text("Priorytet") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategoria") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val updated = original.copy(
                        title = title,
                        description = description,
                        priority = priority,
                        category = category
                    )
                    taskViewModel.updateTask(updated)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz zmiany")
            }
        }
    } ?: Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Ładowanie zadania...")
    }
}
