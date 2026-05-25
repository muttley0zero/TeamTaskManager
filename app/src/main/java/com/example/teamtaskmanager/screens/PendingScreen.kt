package com.example.teamtaskmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.teamtaskmanager.auth.AuthViewModel

@Composable
fun PendingScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()  // <- tu pobieramy ViewModel
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Twoje konto oczekuje na zatwierdzenie przez szefa.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Skontaktuj się z właścicielem zespołu, aby zatwierdził Twoje zgłoszenie.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    // Wylogowanie użytkownika
                    authViewModel.logout()
                    // Ponowna nawigacja do login (czyści backstack)
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cofnij do logowania")
            }
        }
    }
}
