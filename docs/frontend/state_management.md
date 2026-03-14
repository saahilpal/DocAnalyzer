# State Management: DocAnalyzer

The application leverages a reactive, unidirectional state management pattern using Kotlin `StateFlow` and Jetpack Compose's state observation.

## 1. UI State Holders (ViewModels)
Each major feature has a dedicated `UiState` data class that encapsulates all information needed to render the screen. This ensures the UI is a pure function of the state.

- **`ChatUiState`:** Manages a complex set of properties including the list of chat sessions, current messages (mapped by session ID), active upload progress, and visibility toggles for dialogs/sheets.
- **`AuthUiState`:** Manages form inputs (email, password, OTP), validation error messages, and the overall authentication status.

## 2. Reactive Data Streams (`StateFlow`)
- **Observation:** ViewModels expose a `StateFlow` (e.g., `val uiState: StateFlow<ChatUiState>`).
- **Consumption:** The UI layer consumes this via `uiState.collectAsState()` (or `collectAsStateWithLifecycle()` in production environments), which triggers recomposition whenever any property in the state object changes.
- **Updates:** State is updated using the `.update { it.copy(...) }` pattern, ensuring atomicity and thread safety.

## 3. Handling List State (`SnapshotStateList`)
For the chat message history, the app uses `SnapshotStateList` (via `mutableStateListOf()`) stored within a Map in the `ChatViewModel`.
- **Reason:** This allows for efficient, granular updates to the message list (like appending a token to a streaming message) without triggering a full copy and recomposition of the entire list object, which is critical for performance during real-time AI streaming.

## 4. Lifecycle-Aware Operations
- **`viewModelScope`:** All state-changing asynchronous operations (network calls, polling) are launched in the `viewModelScope`, ensuring they are automatically cancelled if the user navigates away and the ViewModel is cleared.
- **Launch State:** Initial configuration and splash screen transitions are managed in the `MainActivity` using `lifecycleScope` and local `mutableStateOf` variables.

## 5. Global State vs Local State
- **Global:** Authentication status and user profile are treated as global states, often shared or synchronized between `AuthViewModel` and `ChatViewModel`.
- **Local:** Form inputs and UI-only toggles (like `showRenameDialog`) are strictly local to the feature's ViewModel.
- **Internal:** Scroll positions and drawer states are managed using Compose's internal `rememberLazyListState()` and `rememberDrawerState()` to keep the UI layer responsive.
