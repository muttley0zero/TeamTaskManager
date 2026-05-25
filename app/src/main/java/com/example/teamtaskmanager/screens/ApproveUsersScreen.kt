package com.example.teamtaskmanager.screens

import android.R.attr.navigationIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.viewmodel.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveUsersScreen(authViewModel: AuthViewModel, navController: NavHostController) {
    val appUser by authViewModel.appUser.collectAsState()
    val firestore = FirebaseFirestore.getInstance()

    val pendingUsers = remember { mutableStateListOf<Pair<String, User>>() }
    val approvedUsers = remember { mutableStateListOf<Pair<String, User>>() }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    // Load pending and approved users when appUser changes
    LaunchedEffect(appUser) {
        val currentUser = appUser
        if (currentUser != null && currentUser.role == "boss") {
            val teamId = currentUser.teamId
            // 1. Pobierz pending users
            val teamDoc = firestore.collection("teams").document(teamId).get().await()
            val pendingIds = teamDoc.get("pendingUsers") as? List<String> ?: emptyList()
            pendingUsers.clear()
            pendingIds.forEach { uid ->
                val doc = firestore.collection("users").document(uid).get().await()
                doc.toObject(User::class.java)?.let { pendingUsers.add(uid to it) }
            }
            // 2. Pobierz approved users
            val approvedSnapshot = firestore.collection("users")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("status", "approved")
                .get().await()
            approvedUsers.clear()
            approvedUsers.addAll(approvedSnapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.let { doc.id to it }
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zatwierdź użytkowników") },
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
                .padding(16.dp)
        ) {
            // === CZĘŚĆ 1: PENDING USERS ===
            Text(
                "Użytkownicy oczekujący na zatwierdzenie",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))

            if (pendingUsers.isEmpty()) {
                Text("Brak użytkowników do zatwierdzenia.")
            } else {
                pendingUsers.forEach { (uid, user) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Email użytkownika po lewej
                        Text(
                            text = user.email,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row {
                            // Przycisk zatwierdź
                            Button(onClick = {
                                authViewModel.approveUser(uid, appUser!!.teamId) {
                                    pendingUsers.remove(uid to user)
                                    approvedUsers.add(uid to user.copy(status = "approved"))
                                }
                            }) {
                                Text("Zatwierdź")
                            }
                            Spacer(Modifier.width(8.dp))
                            // Przycisk odrzuć (reject)
                            OutlinedButton(onClick = {
                                authViewModel.rejectUser(uid, appUser!!.teamId) {
                                    pendingUsers.remove(uid to user)
                                }
                            }) {
                                Text("Odrzuć")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // === CZĘŚĆ 2: APPROVED USERS ===
            Text(
                "Zatwierdzeni członkowie zespołu",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))

            if (approvedUsers.isEmpty()) {
                Text("Brak zatwierdzonych użytkowników.")
            } else {
                approvedUsers.forEach { (uid, user) ->
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Email i rola użytkownika
                        Text(
                            text = "${user.email} (${user.role})",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (uid != currentUid) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Zmień rolę
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Filled.Person, contentDescription = "Zmień rolę")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("worker", "lead", "guest").forEach { roleOption ->
                                        DropdownMenuItem(
                                            text = { Text("Ustaw jako $roleOption") },
                                            onClick = {
                                                firestore.collection("users").document(uid)
                                                    .update("role", roleOption)
                                                approvedUsers.replaceAll {
                                                    if (it.first == uid) uid to it.second.copy(role = roleOption)
                                                    else it
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                // Przycisk usuń (remove) po zatwierdzeniu
                                Button(onClick = {
                                    val userRef = firestore.collection("users").document(uid)
                                    val teamRef = firestore.collection("teams").document(appUser!!.teamId)
                                    val tasksRef = firestore.collection("tasks")
                                    userRef.get().addOnSuccessListener { doc ->
                                        val teamId = doc.getString("teamId") ?: return@addOnSuccessListener
                                        tasksRef.whereEqualTo("assignedToUserId", uid)
                                            .whereEqualTo("teamId", teamId)
                                            .get().addOnSuccessListener { taskSnapshot ->
                                                val batch = firestore.batch()
                                                taskSnapshot.documents.forEach { batch.delete(it.reference) }
                                                batch.update(userRef, mapOf("status" to "removed", "teamId" to ""))
                                                batch.update(teamRef, "pendingUsers", FieldValue.arrayRemove(uid))
                                                batch.commit().addOnSuccessListener {
                                                    approvedUsers.removeIf { it.first == uid }
                                                }
                                            }
                                    }
                                }) {
                                    Text("Usuń")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
