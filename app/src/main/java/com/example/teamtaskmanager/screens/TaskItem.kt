package com.example.teamtaskmanager.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.teamtaskmanager.data.Task
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskItem(
    task: Task,
    isAssignedToUser: Boolean,
    isUserBossOrLead: Boolean,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onTaskClick: () -> Unit,
    onMarkAsCompleted: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onTaskClick() }
    ) {
        // Tytuł zadania
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(text = task.title)
            Text(text = "Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
        }

        // Ikony edycji, usuwania i oznaczania jako zakończone
        Row {
            // Edycja i usuwanie tylko dla przełożonych
            if (isUserBossOrLead) {
                IconButton(onClick = onEditTask) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edytuj zadanie")
                }

                IconButton(onClick = onDeleteTask) {
                    Icon(Icons.Filled.Delete, contentDescription = "Usuń zadanie")
                }
            }

            // Zakończenie zadania dla przypisanego użytkownika
            if (isAssignedToUser) {
                IconButton(onClick = onMarkAsCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = "Zakończ zadanie")
                }
            }
        }
    }
}
