package com.example.teamtaskmanager

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.example.teamtaskmanager.auth.AuthViewModel
import com.example.teamtaskmanager.screens.*
import com.example.teamtaskmanager.viewmodel.TaskViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.appUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != "login" && currentRoute != "register") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            currentUser?.let {
                when {
                    it.role == "boss" -> {
                        // Szef bez zespołu tworzy team, w przeciwnym razie od razu do zadań
                        if (it.teamId.isBlank()) {
                            navController.navigate("create_team") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("task_list") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                    it.role != "boss" -> {
                        // Zwykły użytkownik
                        when (it.status) {
                            "pending" -> {
                                navController.navigate("pending") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            "removed" -> {
                                navController.navigate("enter_team_id") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            "approved" -> {
                                navController.navigate("task_list") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {

        // LOGIN
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                navController = navController,
                onRegisterClicked = { navController.navigate("register") }
            )
        }

        // REGISTER
        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onLoginClicked = { navController.popBackStack() }
            )
        }

        // CREATE TEAM (dla szefa)
        composable("create_team") {
            CreateTeamScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // PENDING SCREEN (oczekujący)
        composable("pending") {
            PendingScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // ENTER TEAM ID (ponowne dołączenie)
        composable("enter_team_id") {
            EnterTeamIdScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // TASK LIST
        composable("task_list") {
            TaskListScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // ADD TASK
        composable("add_task") {
            AddTaskScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // APPROVE USERS
        composable("approve_user") {
            ApproveUsersScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // EDIT TASK
        composable("edit_task/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
            EditTaskScreen(
                navController = navController,
                taskId = taskId
            )
        }

        // TASK DETAILS
        composable("task_details/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable

            // Zamiast przekazywać TaskViewModel, zainicjuj go tutaj za pomocą hiltViewModel()
            val taskViewModel: TaskViewModel = hiltViewModel()

            TaskDetailScreen(
                taskId = taskId,
                navController = navController,
                taskViewModel = taskViewModel
            )
        }

        composable("team_members") {
            TeamMembersScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        composable("task_calendar") {
            TaskCalendarScreen(
                authViewModel = authViewModel,
                navController = navController,
                taskViewModel = hiltViewModel<TaskViewModel>()
            )
        }
    }
}
