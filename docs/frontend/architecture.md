# Architecture: DocAnalyzer

The application implements a robust **MVVM (Model-View-ViewModel)** architecture, optimized for Jetpack Compose and reactive data streams.

## Core Components

### 1. ViewModels
- **Responsibility:** Act as the "State Holder" for the UI. They transform raw data from repositories into a format suitable for the UI (typically a single `UiState` data class).
- **Technology:** `androidx.lifecycle.ViewModel`, `MutableStateFlow`.
- **Key ViewModels:** 
    - `ChatViewModel`: Manages the complex state of chat sessions, messages, and document uploads.
    - `AuthViewModel`: Manages authentication states (login, registration, etc.).

### 2. UI Layer (Jetpack Compose)
- **Responsibility:** Declarative UI that "observes" the ViewModel's state. It is entirely stateless, relying on the ViewModel for all data and logic.
- **Technology:** Jetpack Compose, Material 3.
- **State Observation:** Uses `collectAsState()` to transform `StateFlow` into Compose-friendly `State`.

### 3. Repository Pattern
- **Responsibility:** Manages data operations and provides a clean API to the rest of the app. It decides whether to fetch data from the network or a local cache (though this app primarily uses network).
- **Base Class:** `BaseRepository` provides a `safeApiCall` wrapper to handle exceptions and map Retrofit `Response` objects to a `NetworkResult` sealed class.
- **Key Repositories:**
    - `DocRepository`: Handles session CRUD, PDF uploads, and SSE streaming.
    - `AuthRepository`: Handles all authentication network calls and local token persistence via `AuthManager`.

### 4. Network Layer
- **Retrofit/OkHttp:** Configured as singletons in `RetrofitClient`.
- **Authentication:** `AuthInterceptor` automatically adds the `Authorization: Bearer <token>` header to every request if a token is available in `AuthManager`.
- **SSE (Server-Sent Events):** Implemented using `okhttp3-sse`. The `DocRepository` converts the raw SSE stream into a Kotlin `Flow<ChatStreamEvent>`, which the `ChatViewModel` then collects.

### 5. State Management
- **Uni-directional Data Flow (UDF):** Actions flow up (UI -> ViewModel), and State flows down (ViewModel -> UI).
- **Sealed Classes:** Used for `NetworkResult` and `ChatStreamEvent` to ensure exhaustive handling of all possible states (Success, Error, Loading, etc.).

### 6. Dependency Injection
- The project uses a simplified manual DI approach:
    - `RetrofitClient` acts as a service locator for the `ApiService` and `OkHttpClient`.
    - ViewModels are instantiated via `ViewModelProvider.Factory` (e.g., `AuthViewModelFactory`), which injects the required repositories.

### 7. Coroutines Usage
- **`viewModelScope`:** Used in ViewModels for launching network calls, ensuring they are cancelled when the ViewModel is cleared.
- **`lifecycleScope`:** Used in `MainActivity` for initial configuration fetching.
- **`Dispatchers.IO`:** Used for network and disk I/O operations.
- **`Dispatchers.Main.immediate`:** Used for updating UI state from background threads to ensure immediate consistency.

## UI -> Backend Flow Example
1. **UI:** User types a message and clicks "Send". `ChatViewModel.sendMessage(text)` is called.
2. **ViewModel:** Updates `uiState` to `isSending = true`. Calls `DocRepository.streamChat(...)`.
3. **Repository:** Initiates an SSE request using OkHttp. Listens for events (`token`, `done`, `error`).
4. **Network:** Backend streams response tokens via SSE.
5. **Repository:** Emits `ChatStreamEvent.Token` into a `Flow`.
6. **ViewModel:** Collects the `Flow`. For each `Token` event, it appends the text to the current message in `uiState`.
7. **UI:** Recomposes for every new token, showing the AI response "typing" in real-time.
