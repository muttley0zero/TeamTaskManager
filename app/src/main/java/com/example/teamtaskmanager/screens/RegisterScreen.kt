package com.example.teamtaskmanager.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.teamtaskmanager.auth.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onLoginClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("worker") }
    var expanded by remember { mutableStateOf(false) }
    var teamId by remember { mutableStateOf("") }

    val registrationSuccess by authViewModel.registrationSuccess.collectAsState()
    val errorInformation by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage = errorInformation

    val roles = listOf("boss", "worker", "lead", "guest")

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Rejestracja", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Text(
                text = "Rola: ${selectedRole.replaceFirstChar { it.uppercaseChar() }}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { expanded = true }
                    .padding(12.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
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

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedRole != "boss") {
            OutlinedTextField(
                value = teamId,
                onValueChange = { teamId = it },
                label = { Text("ID zespołu") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                authViewModel.register(
                    email.trim(),
                    password.trim(),
                    selectedRole,
                    teamId.trim()
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Zarejestruj się")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onLoginClicked) {
            Text("Powrót do logowania")
        }

        // komunikat o sukcesie rejestracji
        if (registrationSuccess) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rejestracja udana! Możesz się zalogować.",
                color = MaterialTheme.colorScheme.primary
            )
        }

        // komunikat o błędzie
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
