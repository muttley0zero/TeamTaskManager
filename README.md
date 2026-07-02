# Dokumentacja techniczna aplikacji Team Task Manager  

<div align="center">
  <img src="assets/main_program.jpg" width="200">
</div>

**Autor:** Bartosz Pawlaczyk

---

<a name="spis-tresci"></a>
## Spis treści

1. **[Cel projektu](#cel-projektu)**  
2. **[Główne funkcjonalności](#glowne-funkcjonalnosci)**  
3. **[Wykorzystane biblioteki i technologie](#wykorzystane-biblioteki-i-technologie)**  
4. **[Architektura aplikacji (mvvm)](#architektura-aplikacji-mvvm)**  
5. **[Struktura projektu](#struktura-projektu)**  
6. **[Schemat przeplywu-danych](#schemat-przeplywu-danych)**  
7. **[Pierwsze kroki: Ekrany startowe](#pierwsze-kroki-ekrany-startowe)**

---

## Wprowadzenie

**Team Task Manager** to zaawansowana aplikacja mobilna na system Android, zaprojektowana do efektywnego zarządzania zadaniami wewnątrz zespołów w czasie rzeczywistym. System oferuje pełną synchronizację danych poprzez Firebase, system powiadomień lokalnych i push oraz hierarchiczny podział ról (Szef, Lider, Pracownik).

Aplikacja została zbudowana w oparciu o nowoczesne standardy Androida, wykorzystując:
- Jetpack Compose  
- architekturę MVVM  
- Hilt (dependency injection)

---

<a name="cel-projektu"></a>
## Cel projektu

Głównym założeniem było stworzenie narzędzia eliminującego opóźnienia w komunikacji zespołowej. Kluczowe cele techniczne:

- **Reaktywność** – UI natychmiast reaguje na zmiany w Firestore (Snapshot Listeners)
- **Skalowalność** – możliwość łatwego dodawania modułów (np. kalendarz, analityka)
- **Bezpieczeństwo** – RBAC (Role-Based Access Control)

**[Powrót do spisu treści](#spis-tresci)**

---

<a id="glowne-funkcjonalnosci"></a>
## Główne funkcjonalności

System składa się z następujących modułów:

- **System autentykacji**
  - rejestracja i logowanie
  - automatyczne przypisanie roli użytkownika

- **Zarządzanie zespołem**
  - tworzenie TeamID
  - system zaproszeń
  - akceptacja członków przez Szefa

- **Tablica zadań**
  - tworzenie, edycja, usuwanie zadań
  - filtrowanie po statusach i priorytetach

- **Powiadomienia**
  - push notifications dla przypisanych zadań

- **Widok kalendarza**
  - wizualizacja deadline’ów

[Powrót do spisu treści](#spis-tresci)

---

<a id="wykorzystane-biblioteki-i-technologie"></a>
## Wykorzystane biblioteki i technologie

- **Kotlin + Coroutines/Flow** – logika asynchroniczna  
- **Jetpack Compose (Material 3)** – UI  
- **Hilt (Dagger)** – dependency injection  
- **Firebase Firestore** – baza danych w czasie rzeczywistym  
- **Firebase Auth** – logowanie i rejestracja  
- **Firebase Cloud Messaging (FCM)** – push notifications  
- **Navigation Compose** – nawigacja  
- **ViewModel + Lifecycle** – zarządzanie stanem UI  
- **Coil** – ładowanie obrazów  

[Powrót do spisu treści](#spis-tresci)

---

<a id="architektura-aplikacji-mvvm"></a>
## Architektura aplikacji (MVVM)

Aplikacja wykorzystuje wzorzec **Model-View-ViewModel**, zapewniający separację warstw.

### 1. View (Widok)
- Jetpack Compose  
- brak logiki biznesowej  
- obserwuje `StateFlow` z ViewModelu  

### 2. ViewModel
- pośrednik między UI a danymi  
- używa `viewModelScope`  
- przechowuje stan UI  

### 3. Model / Repository
- warstwa danych  
- komunikacja z Firebase Firestore  
- mapowanie dokumentów na obiekty `Task`  

[Powrót do spisu treści](#spis-tresci)

---

<a id="struktura-projektu"></a>
## Struktura projektu

<div align="center">
  <img src="assets/schemat3.png" width="300">
</div>

[Powrót do spisu treści](#spis-tresci)

---

<a id="schemat-przeplywu-danych"></a>
## Schemat przepływu danych

<div align="center">
  <img src="assets/schemat1.png" width="800">
</div>

<div align="center">
  <img src="assets/schemat2.png" width="500">
</div>

[Powrót do spisu treści](#spis-tresci)

---

<a id="pierwsze-kroki-ekrany-startowe"></a>
## Pierwsze kroki: Ekrany startowe

### Ekran logowania i rejestracji

Proces onboardingu został zoptymalizowany pod minimalny wysiłek użytkownika.

#### Proces rejestracji:

**1. Szef**
- tworzy nowy zespół  
- otrzymuje unikalny `TeamID`

**2. Pracownik**
- wpisuje `TeamID`  
- trafia do listy oczekujących (pending)  
- wymaga akceptacji Szefa  

[Powrót do spisu treści](#spis-tresci)

# Implementacja kluczowych komponentów i logika biznesowa
  
## Inicjalizacja aplikacji i mechanizm Hilt

Projekt wykorzystuje bibliotekę **Hilt** do zarządzania zależnościami.  
Pierwszym krokiem jest konfiguracja klasy bazowej aplikacji, która inicjalizuje graf obiektów.

---

### MyApp.kt

Klasa dziedzicząca po `Application`, oznaczona adnotacją `@HiltAndroidApp`.

Jest to niezbędne, aby Hilt mógł wygenerować kod potrzebny do wstrzykiwania zależności w całym projekcie.

```kotlin
package com.example.teamtaskmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application()
```

---

### FirebaseModule.kt

Moduł Hilt odpowiedzialny za dostarczanie instancji usług Firebase do repozytoriów i modeli widoku. Dzięki temu unikamy ręcznego tworzenia instancji w każdej klasie.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideRealtimeDb(): FirebaseDatabase = FirebaseDatabase.getInstance()
}
```

---

## Główna aktywność i uprawnienia

MainActivity służy jako kontener dla interfejsu Compose. Odpowiada również za konfigurację systemową, taką jak kanały powiadomień i prośby o uprawnienia.

---

### MainActivity.kt

Zarządzanie powiadomieniami i uprawnieniami

Aplikacja musi obsługiwać powiadomienia lokalne oraz Push. W systemie Android 13+ wymagana jest jawna zgoda użytkownika na wyświetlanie powiadomień.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        askNotificationPermission()
        enableEdgeToEdge()
        setContent {
            TeamTaskManagerTheme {
                AppNavGraph()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Zadania Zespołu"
            val channel = NotificationChannel("task_channel_v2", name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Powiadomienia o nowych zadaniach"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
```

---

## System nawigacji i sterowanie rolami

Aplikacja wykorzystuje dynamiczną nawigację opartą na stanie użytkownika. Zamiast prostego grafu przejść, AppNavGraph reaguje na zmiany w profilu użytkownika (rola, status, przynależność do zespołu)

---

### AppNavGraph.kt

        }
    }
}
Logika przekierowań (Redirection Logic)

Najważniejszym elementem nawigacji jest LaunchedEffect, który obserwuje stan currentUser. System automatycznie decyduje, gdzie skierować użytkownika po zalogowaniu.

```kotlin
LaunchedEffect(currentUser) {
    if (currentUser == null) {// Brak sesji - powrót do logowania
        navController.navigate("login") { popUpTo(0) { inclusive = true } }
    } else {
        currentUser?.let { user ->
            when {
                user.role == "boss" -> {
                    if (user.teamId.isBlank()) navController.navigate("create_team")
                    else navController.navigate("task_list")
                }
                user.role != "boss" -> {
                    when (user.status) {
                        "pending" -> navController.navigate("pending")
                        "approved" -> navController.navigate("task_list")
                        "removed" -> navController.navigate("enter_team_id")
                    }
                }
            }
        }
    }
}
```

        }
    }
}
Definicja tras (NavHost)

Graf nawigacji definiuje trasy z argumentami, co pozwala na przekazywanie np. identyfikatora zadania do ekranu edycji.

```kotlin
NavHost(navController = navController, startDestination = "login") {
    composable("login") { LoginScreen(authViewModel, navController, ...) }
    composable("task_list") { TaskListScreen(authViewModel, navController) }
    
    // Trasa z argumentem dla szczegółów zadania
    composable("task_details/{taskId}") { backStackEntry ->
        val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
        TaskDetailScreen(taskId, navController, hiltViewModel())
    }
    
    composable("task_calendar") {
        TaskCalendarScreen(authViewModel, navController, hiltViewModel())
    }
}
```

---

## Zarządzanie autentykacją i profilem

AuthViewModel jest sercem operacji na kontach użytkowników. Integruje on Firebase Auth z bazą Firestore, zapewniając spójność danych.

---

### AuthViewModel.kt

Proces rejestracji i inicjalizacji ról
Podczas rejestracji system tworzy dokument użytkownika w Firestore z przypisanym statusem (pending dla pracowników lub automatyczne tworzenie zespołu dla szefa).

```kotlin
fun register(email: String, pass: String, name: String, role: String?) {
    _isLoading.value = true
    auth.createUserWithEmailAndPassword(email, pass)
        .addOnSuccessListener { res ->
            val uid = res.user?.uid ?: ""
            if (role == "boss") {
                createTeamForBoss(uid, name)
            } else {
                createUserAndPending(uid, email, name, role)
            }
        }
        .addOnFailureListener { e ->
            _errorMessage.value = e.message
            _isLoading.value = false
        }
}
```

Synchronizacja stanu użytkownika (Real-time Sync)
ViewModel utrzymuje StateFlow<User?>, który jest zsynchronizowany z dokumentem w Firestore. Jeśli szef zmieni status pracownika na approved, UI pracownika natychmiast zareaguje dzięki mechanizmowi snapshot listenera.

```kotlin
private fun fetchUserFromFirestore(uid: String) {
    firestore.collection("users").document(uid)
        .addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val user = snapshot?.toObject(User::class.java)
            _appUser.value = user
        }
}
```

Powiadomienia Push (FCM)
Obsługa powiadomień zdalnych odbywa się poprzez serwis działający w tle, który przechwytuje zdarzenia z Firebase Cloud Messaging.

---

### MyFirebaseMessagingService.kt

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "task_channel_v2")
            .setContentTitle(remoteMessage.notification?.title)
            .setContentText(remoteMessage.notification?.body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random.nextInt(), notification)
    }
}
```

[Powrót do spisu treści](#spis-tresci)

#Zarządzanie danymi i logika ViewModeli
  
## Architektura warstwy danych

Serce aplikacji stanowi repozytorium, które abstrahuje operacje na bazie danych Cloud Firestore. Dzięki zastosowaniu Kotlin Flow, dane są strumieniowane bezpośrednio z chmury do interfejsu użytkownika.

---

### TaskRepository.kt

Repozytorium odpowiada za wszystkie operacje CRUD na zadaniach oraz za dostarczanie strumieni danych w czasie rzeczywistym.

Pobieranie zadań w czasie rzeczywistym

Metoda getTaskSnapshotByTeam wykorzystuje callbackFlow oraz addSnapshotListener, co pozwala na nasłuchiwanie zmian w konkretnej kolekcji Firestore przefiltrowanej pod kątem identyfikatora zespołu.

```kotlin
fun getTaskSnapshotByTeam(teamId: String): Flow<QuerySnapshot> = callbackFlow {
    val snapshotListener = firestore.collection("tasks")
        .whereEqualTo("teamId", teamId)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot)
            }
        }
    awaitClose { snapshotListener.remove() }
}
```

Operacje modyfikacji danych
Metody takie jak updateTaskStatus wykorzystują asynchroniczne wywołania zawieszone (suspend), co zapewnia nieblokujące działanie aplikacji podczas komunikacji sieciowej.

```kotlin
suspend fun updateTaskStatus(taskId: String, newStatus: String) {
    try {
        val taskRef = firestore.collection("tasks").document(taskId)
        taskRef.update("status", newStatus).await()
    } catch (e: Exception) {
        throw e
    }
}
```

---

### TaskViewModel.kt

TaskViewModel zarządza stanem listy zadań, błędami oraz wyzwalaniem powiadomień lokalnych. Jest on wstrzykiwany do ekranów Compose za pomocą Hilt.

Inicjalizacja i ładowanie zadań

ViewModel zarządza subskrypcją danych tak, aby nie tworzyć nadmiarowych połączeń przy rekonfiguracji ekranu. Wykorzystuje viewModelScope do zarządzania cyklem życia strumienia danych.

```kotlin
fun loadTasks(teamId: String, currentUserId: String) {
    if (currentLoadedUid == currentUserId && currentLoadedTeamId == teamId) return
    
    taskJob?.cancel()
    currentLoadedUid = currentUserId
    currentLoadedTeamId = teamId

    taskJob = viewModelScope.launch {
        repository.getTaskSnapshotByTeam(teamId).collect { snap ->
            val list = snap.documents.mapNotNull { it.toObject(Task::class.java) }
            processTaskNotifications(snap, currentUserId)
            _tasks.value = list
        }
    }
}
```

Logika powiadomień lokalnych

Aplikacja implementuje mechanizm wykrywania nowych zadań przypisanych do użytkownika. Wykorzystuje do tego SharedPreferences, aby przechowywać znacznik czasu ostatniej kontroli (last_check), co zapobiega dublowaniu powiadomień po ponownym uruchomieniu aplikacji.

Warunki wyzwolenia powiadomienia:

1. Zadanie zostało dodane (DocumentChange.Type.ADDED).
2. Zadanie jest przypisane do bieżącego użytkownika (assignedToUserId).
3. Autorem zadania jest inna osoba (createdBy != currentUserId).
4. Czas utworzenia zadania jest nowszy niż ostatnia synchronizacja.

```kotlin
private fun sendLocalNotification(task: Task) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, "task_channel_v2")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Nowe zadanie: ${task.title}")
        .setContentText(task.description)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(task.id.hashCode(), notification)
}
```

    notificationManager.notify(task.id.hashCode(), notification)
}
Zarządzanie zadaniami (CRUD)
ViewModel udostępnia metody do manipulacji zadaniami, które automatycznie obsługują błędy i aktualizują stan widoku.

```kotlin
fun updateTaskStatus(taskId: String, newStatus: String) {
    viewModelScope.launch {
        try {
            repository.updateTaskStatus(taskId, newStatus)
        } catch (e: Exception) {
            _errorMessage.value = "Błąd aktualizacji: ${e.message}"
        }
    }
}
```

---

## Modele danych i mapowanie

Aplikacja wykorzystuje klasy danych (data classes) Kotlin do reprezentowania struktury dokumentów w Firestore. Domyślne wartości parametrów są wymagane przez deserializator Firebase.

---

### Task.kt

Model zadania zawiera metadane dotyczące czasu utworzenia (createdAt), co jest kluczowe dla poprawnego działania sortowania oraz systemu powiadomień.

```kotlin
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedToUserId: String = "",
    val createdBy: String = "",
    val teamId: String = "",
    val status: String = "open", // open, in_progress, completed
    val priority: String = "medium",
    val dueDate: Long = 0L,
    val createdAt: Long = 0L
)
```

[Powrót do spisu treści](#spis-tresci)

#Interfejs użytkownika i ekrany aplikacji
  
## Projektowanie interfejsu w Jetpack Compose

Interfejs aplikacji został zaprojektowany zgodnie z wytycznymi Material Design 3. Zastosowano podejście deklaratywne, w którym interfejs jest funkcją stanu dostarczanego przez ViewModel. Wszystkie komponenty UI są reaktywne i automatycznie odświeżają się po zmianie danych w StateFlow.

---

## Ekran logowania i rejestracji

Ekrany te stanowią punkt wejścia do aplikacji. Wykorzystują one AuthViewModel do komunikacji z Firebase Authentication.

### LoginScreen.kt

Ekran logowania obsługuje walidację pól tekstowych oraz wyświetlanie błędów autoryzacji.
Ekran logowania
Implementacja formularza logowania:
Zastosowano komponenty OutlinedTextField z obsługą ukrywania hasła (PasswordVisualTransformation).

```kotlin
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,    navController: NavController,
    onRegisterClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by authViewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Team Task Manager", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White)
            else Text("Zaloguj")
        }
    }
}
```

---

## Główna lista zadań

Ekran TaskListScreen jest centralnym punktem aplikacji dla zalogowanego użytkownika. Wyświetla on zadania pobrane z Firestore i przefiltrowane dla danego zespołu.

---

### TaskListScreen.kt

Lista zadań

Zarządzanie listą i akcjami:

Ekran wykorzystuje Scaffold z FloatingActionButton do dodawania nowych zadań oraz LazyColumn do wydajnego wyświetlania listy.

```kotlin
@Composable
fun TaskListScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val taskViewModel: TaskViewModel = hiltViewModel()
    val tasks by taskViewModel.tasks.collectAsState()
    val user by authViewModel.appUser.collectAsState()

    LaunchedEffect(user) {
        user?.let { taskViewModel.loadTasks(it.teamId, it.uid) }
    }

    Scaffold(
        floatingActionButton = {
            if (user?.role == "boss" || user?.role == "lead") {
                FloatingActionButton(onClick = { navController.navigate("add_task") }) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj zadanie")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    onClick = { navController.navigate("task_details/${task.id}") }
                )
            }
        }
    }
}
```

---

## Szczegóły i edycja zadania

Ekrany te pozwalają na głęboką interakcję z pojedynczym rekordem danych. TaskDetailScreen dynamicznie zmienia dostępne akcje w zależności od roli użytkownika (np. tylko Szef może usunąć zadanie).

---

### TaskDetailScreen.kt

Zmiana statusu zadania:
Użytkownik może przesuwać zadanie między stanami (Otwarty -> W toku -> Zakończony).

```kotlin
@Composable
fun TaskStatusRow(
    currentStatus: String,
    onStatusChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val statuses = listOf("open", "in_progress", "completed")
        statuses.forEach { status ->
            FilterChip(
                selected = currentStatus == status,
                onClick = { onStatusChange(status) },
                label = { Text(status.replace("_", " ").capitalize()) }
            )
        }
    }
}
```

---

## Kalendarz zadań

Moduł kalendarza wizualizuje terminy zadań na interaktywnej siatce. Wykorzystuje on dueDate z modelu Task do mapowania obiektów na konkretne dni.

---

### TaskCalendarScreen.kt

Widok kalendarza
Logika mapowania dat:
System grupuje zadania według dni, co pozwala na szybki podgląd obciążenia pracą w danym terminie.

```kotlin
val tasksByDay = tasks.filter { it.dueDate != 0L }.groupBy {
    val cal = Calendar.getInstance().apply { timeInMillis = it.dueDate }
    cal.get(Calendar.DAY_OF_YEAR)
}
```

---

## Zarządzanie zespołem

Ekrany ApproveUsersScreen oraz TeamMembersScreen służą do administracji strukturą zespołu. Szef zespołu ma możliwość akceptowania nowych członków lub usuwania obecnych.

---

### ApproveUsersScreen.kt

Proces zatwierdzania:
Wyświetla listę użytkowników ze statusem pending. Po kliknięciu "Akceptuj", status użytkownika w Firestore zmienia się na approved, co automatycznie odblokowuje mu dostęp do zadań poprzez AppNavGraph.

```kotlin
Button(onClick = { authViewModel.approveUser(pendingUser.uid, teamId) }) {
    Text("Zatwierdź użytkownika")
}
```

[Powrót do spisu treści](#spis-tresci)

#Integracja z usługami Firebase i warstwa danych

## Wstęp do usług chmurowych

Aplikacja opiera swoją funkcjonalność na ekosystemie Firebase, co pozwala na rezygnację z klasycznego serwera backendowego. Wykorzystanie rozwiązań Serverless zapewnia automatyczne skalowanie oraz natychmiastową synchronizację danych pomiędzy użytkownikami.

---

## Firebase Authentication

Moduł ten odpowiada za bezpieczne zarządzanie tożsamością użytkowników. Aplikacja wykorzystuje metodę autoryzacji za pomocą adresu e-mail oraz hasła.
Proces rejestracji i logowania
Podczas tworzenia konta, Firebase Auth generuje unikalny identyfikator UID, który służy jako klucz główny w relacjach z dokumentami w bazie Firestore.

```kotlin
// Przykład wywołania logowania w AuthViewModel
auth.signInWithEmailAndPassword(email, password)
    .addOnSuccessListener { result ->
        val uid = result.user?.uid
        if (uid != null) {
            fetchUserFromFirestore(uid)
        }
    }
```

---

## Cloud Firestore - Struktura bazy danych

Baza danych została zaprojektowana w modelu dokumentowym (NoSQL). Struktura opiera się na trzech głównych kolekcjach, które są ze sobą powiązane za pomocą identyfikatorów.
1. Kolekcja: users
Przechowuje profile użytkowników oraz ich aktualne role w systemie.

Struktura dokumentu:

• uid: String (ID z Firebase Auth)
• email: String
• name: String
• role: String (boss, lead, worker)
• status: String (pending, approved, removed)
• teamId: String (powiązanie z kolekcją teams)
• fcmToken: String (klucz do wysyłania powiadomień Push)

2. Kolekcja: teams
Definiuje strukturę zespołów.
Struktura dokumentu:

• teamId: String (unikalny kod zespołu)
• bossId: String (UID twórcy zespołu)
• teamName: String
• pendingUsers: List<String> (lista UID osób oczekujących na akceptację)

3. Kolekcja: tasks
Zawiera szczegółowe informacje o zadaniach.
Struktura dokumentu:

• id: String
• title: String
• description: String
• assignedToUserId: String
• createdBy: String
• teamId: String
• status: String (open, in_progress, completed)
• priority: String (low, medium, high)
• dueDate: Long (timestamp)
• createdAt: Long (timestamp)

---

## Firebase Cloud Messaging (FCM)

Aplikacja wykorzystuje FCM do obsługi powiadomień Push. Każde urządzenie po zalogowaniu rejestruje unikalny token, który jest zapisywany w profilu użytkownika w Firestore.
Mechanizm działania
1. Podczas uruchomienia aplikacji sprawdzany jest aktualny token urządzenia.
2. Token jest aktualizowany w dokumentach użytkownika.
3. Serwer lub zewnętrzna funkcja (np. Cloud Functions) może wysyłać komunikaty na konkretny token w przypadku przypisania zadania.

```kotlin
// Aktualizacja tokena w AuthViewModel
fun updateFcmToken(token: String) {
    val uid = auth.currentUser?.uid ?: return
    firestore.collection("users").document(uid)
        .update("fcmToken", token)
        .addOnFailureListener { e ->
            Log.e("AuthViewModel", "Błąd aktualizacji tokena FCM", e)
        }
}
```

---

## Repozytorium i strumienie danych

Warstwa Repository stanowi pomost między Firestore a ViewModelami. Zamiast jednorazowych zapytań, aplikacja preferuje nasłuchiwanie zmian (Snapshot Listeners), co zapewnia reaktywność interfejsu.
Implementacja Flow w TaskRepository
Wykorzystanie callbackFlow pozwala na przekształcenie asynchronicznych listenerów Firebase w reaktywne strumienie Kotlin Flow.

```kotlin
fun getAllTasks(teamId: String): Flow<List<Task>> = callbackFlow {
    val listener = firestore.collection("tasks")
        .whereEqualTo("teamId", teamId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val tasks = snapshot?.documents?.mapNotNull { 
                it.toObject(Task::class.java) 
            } ?: emptyList()
            trySend(tasks)
        }
    awaitClose { listener.remove() }
}
```

---

## Schemat przepływu danych (Data Flow)

Struktura przepływu informacji w systemie:
1. Wyzwalacz (Trigger): Użytkownik A zmienia status zadania.
2. Aktualizacja (Update): TaskViewModel wysyła żądanie do TaskRepository, które aktualizuje dokument w Firestore.
3. Synchronizacja (Sync): Firestore rozsyła powiadomienie do wszystkich klientów obserwujących dany teamId.
4. Reakcja (React): TaskViewModel u Użytkownika B otrzymuje nowy snapshot, aktualizuje stan StateFlow, co powoduje automatyczne odświeżenie komponentu TaskCard w interfejsie Compose.

[Powrót do spisu treści](#spis-tresci)

# Instalacja i konfiguracja środowiska

## Wymagania wstępne

Aby poprawnie skompilować i uruchomić projekt, środowisko programistyczne musi spełniać następujące wymagania:
1. Android Studio: Zalecana najnowsza stabilna wersja (np. Ladybug lub nowsza).
2. JDK (Java Development Kit): Wersja 17 lub nowsza. Android Studio zazwyczaj dostarcza własną wersję JDK, która jest wystarczająca.
3. Android SDK: API na poziomie minimum 26 (Android 8.0), zalecane API 34 (Android 14) do pełnej obsługi uprawnień powiadomień.
4. Gradle: Projekt wykorzystuje system budowania Gradle z konfiguracją zapisaną w języku Kotlin (Kotlin DSL).

---

## Przygotowania projektu

1.Pobierz lub sklonuj repozytorium z kodem źródłowym na dysk lokalny.
2. Otwórz Android Studio.
3. Wybierz opcję File > Open i wskaż folder główny projektu (zawierający plik settings.gradle.kts).
4. Poczekaj na zakończenie procesu synchronizacji Gradle. Pierwsza synchronizacja może potrwać kilka minut ze względu na konieczność pobrania zależności.

---

## Konfiguracja usług Firebase

Aplikacja wymaga poprawnego powiązania z projektem w konsoli Firebase. Bez tego kroku funkcje logowania, bazy danych oraz powiadomień nie będą działać.

Kroki konfiguracji w konsoli Firebase:
1. Zaloguj się do Firebase Console.
2. Utwórz nowy projekt o nazwie TeamTaskManager.
3. Dodaj nową aplikację typu Android do projektu.
4. Podaj nazwę pakietu (Package Name): com.example.teamtaskmanager. Musi być ona identyczna z wartością applicationId w pliku app/build.gradle.kts.
5. Pobierz wygenerowany plik google-services.json.
6. Umieść plik google-services.json w katalogu /app/ swojego projektu na dysku.

Aktywacja usług w konsoli:
1. Authentication: Włącz metodę logowania Adres e-mail i hasło.
2. Cloud Firestore: Utwórz bazę danych w trybie produkcyjnym lub testowym i wybierz lokalizację serwera (np. europe-west).
3. Cloud Messaging: Usługa jest domyślnie aktywna, ale warto sprawdzić ustawienia w zakładce Project Settings > Cloud Messaging.

---

## Budowanie i uruchamianie

Tryb Debug
Jest to podstawowy tryb służący do testowania aplikacji w trakcie rozwoju.
1. Podłącz urządzenie fizyczne z włączonym debugowaniem USB lub uruchom emulator (AVD).
2. W górnym panelu Android Studio wybierz moduł app.
3. Kliknij ikonę zielonej strzałki (Run) lub użyj skrótu klawiszowego Shift + F10.
4. Aplikacja zostanie skompilowana, przesłana na urządzenie i uruchomiona.

Tryb Release
Służy do generowania finalnego pakietu instalacyjnego.
1. Przejdź do menu Build > Generate Signed Bundle / APK.
2. Wybierz opcję APK lub Android App Bundle.
3. Utwórz nowy klucz podpisywania (KeyStore) lub wybierz istniejący.
4. Wybierz wariant budowania release.
5. Po zakończeniu procesu plik instalacyjny znajdzie się w folderze app/release/.

---

## Najczęstsze problemy i rozwiązania

**Błędy synchronizacji Gradle**
• Problem: "Build failed" podczas otwierania projektu.
• Rozwiązanie: Sprawdź połączenie z internetem. Wybierz File > Invalidate Caches... i zaznacz opcję restartu. Upewnij się, że wersja JDK w ustawieniach (Settings > Build, Execution, Deployment > Build Tools > Gradle) to minimum 17.

**Aplikacja zamyka się zaraz po starcie**
• Problem: Crash przy próbie inicjalizacji Firebase.
• Rozwiązanie: Upewnij się, że plik google-services.json znajduje się dokładnie w folderze app/ i nie został zmieniony (np. nazwa pliku nie zawiera cyfr w nawiasach po pobraniu kolejnej wersji).

**Brak powiadomień o zadaniach**
• Problem: Zadania są dodawane, ale powiadomienia nie pojawiają się na pasku.
• Rozwiązanie:
  ◦ Sprawdź, czy w systemie Android nadano uprawnienie do powiadomień dla aplikacji.
  ◦ Zweryfikuj, czy urządzenie posiada dostęp do usług Google Play.
  ◦ Sprawdź w logach (Logcat), czy nie występuje błąd MESSAGING_EVENT.

**Firestore: Permission Denied**
• Problem: Aplikacja nie może pobrać lub zapisać zadań.
• Rozwiązanie: Sprawdź reguły bezpieczeństwa w konsoli Firebase. Upewnij się, że zezwalają one na odczyt i zapis zalogowanym użytkownikom. Przykład podstawowej reguły:

```kotlin
  service cloud.firestore {
    match /databases/{database}/documents {
      match /{document=**} {
        allow read, write: if request.auth != null;
      }
    }
  }
```

[Powrót do spisu treści](#spis-tresci)

# Dokumentacja techniczna kodu

---

## Warstwa danych i repozytoria

Warstwa ta odpowiada za bezpośrednią komunikację z API Firebase. Wykorzystuje programowanie reaktywne (Kotlin Flow) oraz asynchroniczne (Coroutines).

### TaskRepository.kt
Klasa zarządzająca operacjami na kolekcji tasks w Cloud Firestore.

**Metoda getTaskSnapshotByTeam**

Tworzy strumień danych (Flow) nasłuchujący na zmiany w zadaniach przypisanych do konkretnego zespołu.

```kotlin
fun getTaskSnapshotByTeam(teamId: String): Flow<QuerySnapshot> = callbackFlow {
    val snapshotListener = firestore.collection("tasks")
        .whereEqualTo("teamId", teamId)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot)
            }
        }
    awaitClose { snapshotListener.remove() }
}
```

**Metoda updateTaskStatus**
Aktualizuje pole status dokumentu zadania. Jest to funkcja zawieszona (suspend), co wymaga wywołania z zakresu korutyny.

```kotlin
suspend fun updateTaskStatus(taskId: String, newStatus: String) {
    try {
        val taskRef = firestore.collection("tasks").document(taskId)
        taskRef.update("status", newStatus).await()
    } catch (e: Exception) {
        throw e
    }
}
```

---

## Warstwa logiki biznesowej (ViewModels)
ViewModele przechowują stan interfejsu użytkownika i pośredniczą w wymianie danych między widokiem a repozytorium.

### AuthViewModel.kt

Zarządza procesem uwierzytelniania oraz strukturą zespołu (role, statusy).

Funkcja createTeamForBoss

Wywoływana podczas rejestracji użytkownika z rolą boss. Tworzy nowy dokument w kolekcji teams i generuje unikalny teamId.

```kotlin
private fun createTeamForBoss(uid: String, userName: String) {
    val teamId = UUID.randomUUID().toString().take(8)
    val teamData = hashMapOf(
        "teamId" to teamId,
        "bossId" to uid,
        "teamName" to "Zespół $userName"
    )
    firestore.collection("teams").document(teamId).set(teamData)
        .addOnSuccessListener {
            updateUserTeamInfo(uid, teamId, "boss", "approved")
        }
}
```
**Funkcja approveUser**
Pozwala szefowi zespołu na zmianę statusu oczekującego użytkownika z pending na approved.

```kotlin
fun approveUser(userUid: String, teamId: String, onSuccess: () -> Unit) {
    firestore.collection("users").document(userUid)
        .update("status", "approved")
        .addOnSuccessListener {
            onSuccess()
        }
}
```

---

### TaskViewModel.kt
Odpowiada za logikę związaną z listą zadań oraz powiadomieniami lokalnymi.

**Funkcja loadTasks**

Inicjalizuje nasłuchiwanie zadań. Zawiera mechanizm taskJob?.cancel(), który zapobiega wyciekom pamięci i wielokrotnym subskrypcjom.

```kotlin
fun loadTasks(teamId: String, currentUserId: String) {
    if (currentLoadedUid == currentUserId && currentLoadedTeamId == teamId) return
    
    taskJob?.cancel()
    taskJob = viewModelScope.launch {
        repository.getTaskSnapshotByTeam(teamId).collect { snap ->
            val list = snap.documents.mapNotNull { it.toObject(Task::class.java) }
            _tasks.value = list
            // Logika powiadomień procesowana po otrzymaniu snapshotu
            checkAndSendNotifications(snap, currentUserId)
        }
    }
}
```

---

## Serwisy i powiadomienia

### MyFirebaseMessagingService.kt

Obsługuje zdarzenia przychodzące z Firebase Cloud Messaging w tle.

**Metoda onMessageReceived**

Analizuje obiekt RemoteMessage i buduje systemowe powiadomienie NotificationCompat.

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        this, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, "task_channel_v2")
        .setContentTitle(remoteMessage.notification?.title ?: "Nowe zadanie")
        .setContentText(remoteMessage.notification?.body ?: "")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(Random.nextInt(), notification)
}
```

---

## Modele Danych

### User.kt

Klasa odzwierciedlająca dokument użytkownika w Firestore.

```kotlin
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "worker",
    val status: String = "pending",
    val teamId: String = "",
    val fcmToken: String = ""
)
```

---

## Konfiguracja Manifestu

Aplikacja wymaga zdefiniowania uprawnień oraz usług w pliku AndroidManifest.xml.

```manifest
<manifest ...>
    <!-- Uprawnienia -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application ...>
        <!-- Serwis powiadomień FCM -->
        <service
            android:name=".notifications.MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

[Powrót do spisu treści](#spis-tresci)

# Bezpieczeństwo, testowanie i podsumowanie

## Bezpieczeństwo danych w Cloud Firestore
Aplikacja opiera swoje bezpieczeństwo na regułach dostępu (Security Rules) Firebase. Dzięki nim, nawet w przypadku uzyskania dostępu do kluczy API aplikacji, dane są chronione na poziomie serwera.

---

## Implementacja reguł dostępu
Reguły zostały skonfigurowane tak, aby użytkownicy mogli operować wyłącznie na danych swojego zespołu.

```javascript
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Reguły dla kolekcji użytkowników
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    // Reguły dla zadań
    match /tasks/{taskId} {
      allow read: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.teamId == resource.data.teamId;
      
      allow create: if request.auth != null;
      
      allow update, delete: if request.auth != null && (
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['boss', 'lead'] ||
        resource.data.createdBy == request.auth.uid
      );
    }
    
    // Reguły dla zespołów
    match /teams/{teamId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && (
        !exists(/databases/$(database)/documents/teams/{teamId}) ||
        resource.data.bossId == request.auth.uid
      );
    }
  }
}
```

---

## Testowanie aplikacji
    match /teams/{teamId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && (
        !exists(/databases/$(database)/documents/teams/{teamId}) ||
        resource.data.bossId == request.auth.uid
      );
    }
  }
}
Projekt został przygotowany z myślą o testowalności dzięki zastosowaniu wstrzykiwania zależności (Hilt) oraz architektury MVVM, która pozwala na testowanie logiki biznesowej bez uruchamiania interfejsu użytkownika.

**Testy jednostkowe (Unit Tests)**
Testy te koncentrują się na weryfikacji logiki w klasach ViewModel oraz Repository przy użyciu atrap (mocks) usług Firebase.
Przykład testu walidacji zadania:

```kotlin
class TaskViewModelTest {
    private lateinit var viewModel: TaskViewModel
    private val repository = mock(TaskRepository::class.java)

    @Test
    fun `dodanie zadania z pustym tytulem powinno ustawic blad`() {
        val task = Task(title = "", description = "Test")
        viewModel.addTask(task)
        
        assert(viewModel.errorMessage.value != null)
    }
}
```

**Testy instrumentacyjne (UI Tests)**
Wykorzystują bibliotekę Compose UI Test do weryfikacji poprawności wyświetlania elementów na ekranie.

```kotlin
@Test
fun loginScreen_wyświetlaPolaTekstowe() {
    composeTestRule.setContent {
        LoginScreen(...)
    }

    composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hasło").assertIsDisplayed()
}
```

---

## Plany rozwoju projektu
Aplikacja została zaprojektowana w sposób modułowy, co umożliwia jej łatwą rozbudowę o następujące funkcjonalności:
1. Tryb Offline: Implementacja lokalnej bazy danych Room jako warstwy cache dla dokumentów z Firestore, umożliwiająca pracę bez dostępu do sieci.
2. Statystyki Zespołu: Dodanie ekranu analitycznego prezentującego wykresy wydajności pracowników (liczba ukończonych zadań, czas realizacji).
3. Załączniki: Integracja z Firebase Storage w celu umożliwienia dodawania zdjęć i dokumentów do poszczególnych zadań.
4. Chat Zespołowy: Implementacja modułu komunikacji w czasie rzeczywistym wewnątrz każdego zespołu.
5. Integracja z AI: Wykorzystanie modeli językowych do automatycznego generowania opisów zadań na podstawie słów kluczowych.

[Powrót do spisu treści](#spis-tresci)

# Podsumowanie

Aplikacja Team Task Manager stanowi kompletne rozwiązanie do zarządzania pracą zespołową w środowisku mobilnym. Dzięki wykorzystaniu nowoczesnego stosu technologicznego Android (Kotlin, Compose, Hilt, Coroutines) oraz platformy Firebase, system charakteryzuje się wysoką wydajnością, stabilnością oraz niskimi opóźnieniami w synchronizacji danych.
Kluczowe osiągnięcia projektu:
•
Pełna synchronizacja stanów w czasie rzeczywistym (Real-time Sync).
•
Zaawansowany system powiadomień lokalnych i zdalnych (Push).
•
Elastyczny system ról użytkowników (RBAC).
•
Przejrzysty i responsywny interfejs użytkownika oparty na Material 3.
•
Architektura zgodna z zasadami Clean Architecture i SOLID.
Projekt ten może służyć zarówno jako gotowe narzędzie do zarządzania projektami, jak i solidna baza do budowy bardziej złożonych systemów klasy Enterprise.

[Powrót do spisu treści](#spis-tresci)
