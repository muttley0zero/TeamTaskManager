package com.example.teamtaskmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamtaskmanager.auth.AuthViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel = hiltViewModel()) {
    val user by authViewModel.appUser.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var selectedRole by remember { mutableStateOf("worker") }
    var teamId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("boss", "worker", "lead", "guest")

    if (user != null) {
        // Użytkownik zalogowany - pokaz przycisk wylogowania
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Zalogowany jako: ${user?.email ?: "Anonim"}")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { authViewModel.logout() }) {
                Text("Wyloguj się")
            }
        }
    } else {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoginMode by remember { mutableStateOf(true) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLoginMode) "Logowanie" else "Rejestracja",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            if (!isLoginMode) {
                Box {
                    Text(
                        text = "Rola: ${selectedRole.replaceFirstChar { it.uppercaseChar() }}",
                        modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                },
                                text = { Text(role.replaceFirstChar { it.uppercaseChar() }) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (selectedRole != "boss") {
                    TextField(
                        value = teamId,
                        onValueChange = { teamId = it },
                        label = { Text("ID zespołu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Button(
                onClick = {
                    if (isLoginMode) {
                        authViewModel.login(email, password)
                    } else {
                        val teamIdParam = if (selectedRole != "boss") teamId else null
                        authViewModel.register(email, password, selectedRole, teamIdParam)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoginMode) "Zaloguj się" else "Zarejestruj się")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { isLoginMode = !isLoginMode },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (isLoginMode)
                        "Nie masz konta? Zarejestruj się"
                    else
                        "Masz konto? Zaloguj się"
                )
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
