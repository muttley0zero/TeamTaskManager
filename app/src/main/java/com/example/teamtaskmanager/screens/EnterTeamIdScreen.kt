package com.example.teamtaskmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teamtaskmanager.auth.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterTeamIdScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val appUser by authViewModel.appUser.collectAsState()
    val scope = rememberCoroutineScope()

    var teamId by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dołącz do zespołu") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = teamId,
                    onValueChange = { teamId = it },
                    label = { Text("Wprowadź Team ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (teamId.isBlank()) {
                            errorMessage = "Wprowadź prawidłowy Team ID"
                            return@Button
                        }

                        isSubmitting = true
                        errorMessage = null

                        scope.launch {
                            try {
                                authViewModel.rejoinTeam(teamId.trim())
                                navController.navigate("pending") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Błąd: ${e.message}"
                            } finally {
                                isSubmitting = false
                            }
                        }

                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dołącz do zespołu")
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
