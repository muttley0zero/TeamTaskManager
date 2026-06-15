package com.example.teamtaskmanager.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamtaskmanager.viewmodel.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // UID aktualnie zalogowanego użytkownika
    val currentUserUid: String? get() = auth.currentUser?.uid

    // --- Stan aplikacyjny użytkownika ---
    private val _appUser = MutableStateFlow<User?>(null)
    val appUser: StateFlow<User?> = _appUser

    // Flaga sukcesu rejestracji
    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess

    // Komunikat błędu
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Stan ładowania
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Nawigacja: enterTeamId / pending
    private val _navigateToEnterTeamId = MutableStateFlow(false)
    val navigateToEnterTeamId: StateFlow<Boolean> = _navigateToEnterTeamId

    private val _navigateToPendingScreen = MutableStateFlow(false)
    val navigateToPendingScreen: StateFlow<Boolean> = _navigateToPendingScreen

    // --- Lista członków zespołu ---
    private val _teamMembers = MutableStateFlow<List<Pair<String, User>>>(emptyList())
    val teamMembers: StateFlow<List<Pair<String, User>>> = _teamMembers

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            fetchUserFromFirestore(uid)
        }
    }

    /** Logowanie */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email i hasło nie mogą być puste"
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    fetchUserFromFirestore(uid)
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Błąd: Brak identyfikatora użytkownika"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = e.message
            }
    }

    /** Pobiera profil użytkownika i ustawia flagi navigacji */
    private fun fetchUserFromFirestore(uid: String) {
        // reset
        _navigateToEnterTeamId.value = false
        _navigateToPendingScreen.value = false

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                _isLoading.value = false
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    _appUser.value = user
                    // Aktualizacja tokenu FCM przy każdym logowaniu/pobraniu profilu
                    updateFcmToken(uid)
                    when {
                        user.role != "boss" && user.status == "pending" ->
                            _navigateToPendingScreen.value = true
                        user.role != "boss" && user.status == "removed" ->
                            _navigateToEnterTeamId.value = true
                    }
                } else {
                    _errorMessage.value = "Profil użytkownika nie został znaleziony w bazie danych"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Błąd pobierania danych: ${e.message}"
            }
    }

    private fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                firestore.collection("users").document(uid).update("fcmToken", token)
            }
        }
    }

    /** Rejestracja */
    fun register(email: String, password: String, role: String, teamIdInput: String? = null) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email i hasło nie mogą być puste"
            return
        }
        if (role != "boss" && teamIdInput.isNullOrBlank()) {
            _errorMessage.value = "Musisz podać ID zespołu"
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        _registrationSuccess.value = false

        if (role != "boss") {
            // sprawdź istnienie zespołu
            firestore.collection("teams").document(teamIdInput!!)
                .get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        _isLoading.value = false
                        _errorMessage.value = "Zespół o podanym ID nie istnieje"
                    } else createUserAndPending(email, password, role, teamIdInput)
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message
                }
        } else {
            createUserAndPending(email, password, role, null)
        }
    }

    /** Tworzy użytkownika i dodaje do pending lub tworzy team przy bossie */
    private fun createUserAndPending(
        email: String,
        password: String,
        role: String,
        teamIdInput: String?
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { res ->
                val uid = res.user?.uid ?: run {
                    _isLoading.value = false
                    _errorMessage.value = "Brak UID"
                    return@addOnSuccessListener
                }
                val map = hashMapOf(
                    "email" to email,
                    "role" to role,
                    "teamId" to (teamIdInput ?: ""),
                    "status" to if (role == "boss") "approved" else "pending"
                )
                firestore.collection("users").document(uid)
                    .set(map)
                    .addOnSuccessListener {
                        if (role == "boss") {
                            createTeamForBoss(uid)
                            _isLoading.value = false
                            _registrationSuccess.value = true
                        } else {
                            firestore.collection("teams").document(teamIdInput!!)
                                .update("pendingUsers", FieldValue.arrayUnion(uid))
                                .addOnSuccessListener {
                                    _isLoading.value = false
                                    _registrationSuccess.value = true
                                }
                                .addOnFailureListener { e ->
                                    _isLoading.value = false
                                    _errorMessage.value = e.message
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        _errorMessage.value = e.message
                    }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = e.message
            }
    }

    /** Tworzy nowy team i przypisuje bossowi */
    fun createTeamForBoss(uid: String? = null) {
        viewModelScope.launch {
            val userId = uid ?: currentUserUid ?: return@launch
            val newId = "team_" + System.currentTimeMillis()
            val teamData = hashMapOf("bossId" to userId, "pendingUsers" to listOf<String>())
            firestore.collection("teams").document(newId)
                .set(teamData)
                .addOnSuccessListener {
                    firestore.collection("users").document(userId)
                        .update("teamId", newId)
                        .addOnSuccessListener {
                            _appUser.value = _appUser.value?.copy(teamId = newId)
                        }
                }
        }
    }

    /** Zatwierdza użytkownika */
    fun approveUser(userId: String, teamId: String, onSuccess: () -> Unit = {}) {
        firestore.collection("users").document(userId)
            .update("status", "approved")
            .addOnSuccessListener {
                firestore.collection("teams").document(teamId)
                    .update("pendingUsers", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener { onSuccess() }
            }
    }

    /** Wylogowanie */
    fun logout() {
        auth.signOut()
        _appUser.value = null
    }

    /** Odświeża listę członków zespołu */
    fun fetchTeamMembers() {
        val tId = _appUser.value?.teamId ?: return
        viewModelScope.launch {
            try {
                val snap = firestore.collection("users")
                    .whereEqualTo("teamId", tId)
                    .get()
                    .await()
                val list = snap.documents.mapNotNull { d ->
                    d.toObject(User::class.java)?.let { u -> d.id to u }
                }
                _teamMembers.value = list
            } catch (e: Exception) {
                Log.e("AuthVM", "fetchTeamMembers", e)
            }
        }
    }

    /** Rejoin po usunięciu */
    fun rejoinTeam(teamId: String) {
        val uid = currentUserUid ?: return
        val userRef = firestore.collection("users").document(uid)
        userRef.update(mapOf("teamId" to teamId, "status" to "pending", "role" to "worker"))
            .addOnSuccessListener {
                firestore.collection("teams").document(teamId)
                    .update("pendingUsers", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener { fetchUserFromFirestore(uid) }
            }
    }

    /** Odrzucenie prośby */
    fun rejectUser(userId: String, teamId: String, onSuccess: () -> Unit = {}) {
        firestore.collection("users").document(userId)
            .update("status", "removed", "teamId", "")
            .addOnSuccessListener {
                firestore.collection("teams").document(teamId)
                    .update("pendingUsers", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener { onSuccess() }
            }
    }

    fun clearNavigateToEnterTeamId() {
        _navigateToEnterTeamId.value = false
    }

    fun clearNavigateToPendingScreen() {
        _navigateToPendingScreen.value = false
    }

    fun checkTeamExists(teamId: String, callback: (Boolean) -> Unit) {
        val trimmedId = teamId.trim()
        if (trimmedId.isBlank()) {
            _errorMessage.value = "ID zespołu nie może być puste"
            callback(false)
            return
        }
        firestore
            .collection("teams")
            .document(trimmedId)
            .get()
            .addOnSuccessListener { document ->
                callback(document.exists())
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd: ${e.message}"
                callback(false)
            }
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }
}
