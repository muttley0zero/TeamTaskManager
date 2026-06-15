package com.example.teamtaskmanager.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.data.Task
import com.example.teamtaskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditTaskScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    taskId: String,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        authViewModel.fetchTeamMembers()
    }

    val currentUser by authViewModel.appUser.collectAsState()
    val members by authViewModel.teamMembers.collectAsState()
    var task by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(taskId) {
        taskViewModel.getTaskById(taskId).collectLatest { fetchedTask ->
            task = fetchedTask
        }
    }

    task?.let { original ->
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var title by remember { mutableStateOf(original.title) }
        var description by remember { mutableStateOf(original.description) }
        var priority by remember { mutableStateOf(original.priority) }
        var category by remember { mutableStateOf(original.category) }
        var selectedUserId by remember { mutableStateOf(original.assignedToUserId) }
        var startDate by remember {
            mutableStateOf(
                Instant.ofEpochMilli(original.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
            )
        }
        var endDate by remember {
            mutableStateOf(
                Instant.ofEpochMilli(original.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
            )
        }

        var expandedUsers by remember { mutableStateOf(false) }
        var expandedPriority by remember { mutableStateOf(false) }
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        val filteredMembers = when (currentUser?.role) {
            "boss" -> members.filter { it.second.email != currentUser?.email && it.second.status != "pending" }
            "lead" -> members.filter {
                (it.second.role == "worker" || it.second.role == "guest") &&
                        it.second.email != currentUser?.email &&
                        it.second.status != "pending"
            }
            else -> emptyList()
        }
        val uniqueFilteredMembers = filteredMembers.distinctBy { it.second.email }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edytuj zadanie") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wybór użytkownika (tylko dla boss/lead)
                if (currentUser?.role in listOf("boss", "lead")) {
                    ExposedDropdownMenuBox(
                        expanded = expandedUsers,
                        onExpandedChange = { expandedUsers = !expandedUsers },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uniqueFilteredMembers.firstOrNull { it.first == selectedUserId }?.second?.email
                                ?: members.firstOrNull { it.first == selectedUserId }?.second?.email ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Przypisz do") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUsers) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUsers,
                            onDismissRequest = { expandedUsers = false }
                        ) {
                            uniqueFilteredMembers.forEach { (uid, user) ->
                                DropdownMenuItem(
                                    text = { Text(user.email) },
                                    onClick = {
                                        selectedUserId = uid
                                        expandedUsers = false
                                    }
                                )
                            }
                        }
                    }
                }

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

                // Priorytet
                ExposedDropdownMenuBox(
                    expanded = expandedPriority,
                    onExpandedChange = { expandedPriority = !expandedPriority },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priorytet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPriority,
                        onDismissRequest = { expandedPriority = false }
                    ) {
                        listOf("low", "medium", "high").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    priority = opt
                                    expandedPriority = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Data rozpoczęcia
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data rozpoczęcia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showStartPicker = true }
                    )
                }
                if (showStartPicker) {
                    val state = rememberDatePickerState(
                        initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showStartPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                state.selectedDateMillis?.let { millis ->
                                    startDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                }
                                showStartPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartPicker = false }) { Text("Anuluj") }
                        }
                    ) {
                        DatePicker(state = state)
                    }
                }

                // Data zakończenia
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = endDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data zakończenia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showEndPicker = true }
                    )
                }
                if (showEndPicker) {
                    val state = rememberDatePickerState(
                        initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showEndPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                state.selectedDateMillis?.let { millis ->
                                    endDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                }
                                showEndPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndPicker = false }) { Text("Anuluj") }
                        }
                    ) {
                        DatePicker(state = state)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val taskStatus = if (endDate.isBefore(LocalDate.now())) "expired" else original.status
                        val assignedUserRole = members.firstOrNull { it.first == selectedUserId }?.second?.role ?: original.assignedRole

                        val updated = original.copy(
                            title = title,
                            description = description,
                            priority = priority,
                            category = category,
                            assignedToUserId = selectedUserId,
                            assignedRole = assignedUserRole,
                            startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            dueDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            status = taskStatus
                        )
                        taskViewModel.updateTask(updated)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && description.isNotBlank()
                ) {
                    Text("Zapisz zmiany")
                }
            }
        }
    } ?: Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
