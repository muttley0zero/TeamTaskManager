package com.example.teamtaskmanager.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.data.Task
import com.example.teamtaskmanager.viewmodel.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddTaskScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        authViewModel.fetchTeamMembers()
    }

    val currentUser by authViewModel.appUser.collectAsState()
    val members by authViewModel.teamMembers.collectAsState()

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var category by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

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
                title = { Text("Dodaj zadanie") },
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
            // Wybór użytkownika
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = members.firstOrNull { it.first == selectedUserId }?.second?.email
                        ?: "Wybierz użytkownika",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Przypisz do") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedUsers = true }
                )
                DropdownMenu(
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

            // Tytuł i opis
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
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priorytet") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPriority = true }
                )
                DropdownMenu(
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

            // Kategoria
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategoria") },
                modifier = Modifier.fillMaxWidth()
            )

            // Data rozpoczęcia
            OutlinedTextField(
                value = startDate?.format(dateFormatter) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Data rozpoczęcia") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartPicker = true }
            )
            if (showStartPicker) {
                val state = rememberDatePickerState()
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
            OutlinedTextField(
                value = endDate?.format(dateFormatter) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Data zakończenia") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndPicker = true }
            )
            if (showEndPicker) {
                val state = rememberDatePickerState()
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

            Spacer(modifier = Modifier.height(16.dp))

            // Przycisk dodania zadania
            Button(
                onClick = {
                    val taskStatus = if (endDate!!.isBefore(LocalDate.now())) "expired" else "pending"
                    val assignedUserRole = members.firstOrNull { it.first == selectedUserId }?.second?.role ?: "unknown"

                    taskViewModel.addTask(
                        Task(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            assignedToUserId = selectedUserId!!,
                            assignedRole = assignedUserRole, // <- poprawka tutaj!
                            createdBy = authViewModel.currentUserUid!!,
                            teamId = currentUser?.teamId ?: "",
                            status = taskStatus,
                            priority = priority,
                            category = category,
                            startDate = startDate!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            dueDate = endDate!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                    )
                    navController.popBackStack()
                },
                enabled = selectedUserId != null
                        && title.isNotBlank()
                        && description.isNotBlank()
                        && startDate != null
                        && endDate != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dodaj zadanie")
            }
        }
    }
}
