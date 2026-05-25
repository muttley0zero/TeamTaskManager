package com.example.teamtaskmanager.screens

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
import androidx.navigation.NavHostController
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.viewmodel.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMembersScreen(
    authViewModel: AuthViewModel,
    navController: NavHostController
) {
    val appUser by authViewModel.appUser.collectAsState()
    val firestore = FirebaseFirestore.getInstance()
    val members = remember { mutableStateListOf<User>() }

    // only for roles lead, worker, guest
    val role = appUser?.role?.lowercase()
    if (role == null || role == "boss") {
        // navigate back if unauthorized
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    LaunchedEffect(appUser) {
        members.clear()
        appUser?.teamId?.let { teamId ->
            val snapshot = firestore.collection("users")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("status", "approved")
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                .forEach { members.add(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Członkowie zespołu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (members.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Brak członków zespołu.")
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members) { user ->
                    Card(
                        Modifier
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = user.email, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Rola: ${user.role}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
