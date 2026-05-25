package com.example.teamtaskmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teamtaskmanager.auth.AuthViewModel

@Composable
fun CreateTeamScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tworzenie zespołu", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                authViewModel.createTeamForBoss()
                navController.navigate("tasks") {
                    popUpTo("create_team") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Utwórz zespół i przejdź dalej")
        }
    }
}
