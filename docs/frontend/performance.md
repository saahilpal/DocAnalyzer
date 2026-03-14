# Performance Analysis: DocAnalyzer

The application is designed with performance in mind, particularly regarding the high-frequency UI updates required for real-time AI streaming.

## 1. Recomposition Optimization
- **Keyed Lazy Lists:** The `ChatScreen` uses `itemsIndexed(messages, key = { _, msg -> msg.id })`. This ensures that when a new token is appended to a message, Compose only recomposes that specific list item rather than the entire list.
- **SnapshotStateList:** Messages are stored in a `SnapshotStateList`. This allows the ViewModel to mutate the list (appending tokens) in a way that Compose can track efficiently at a granular level.
- **State Partitioning:** The `ChatUiState` is a single data class, but the UI is broken down into smaller composables (`ChatBubble`, `ChatInputBar`) that only receive the specific data they need, limiting the scope of recomposition.

## 2. Network Efficiency
- **Streaming (SSE):** By using SSE instead of standard polling or massive JSON responses, the app reduces latency and avoids the overhead of repeated HTTP handshakes during AI response generation.
- **Multipart Uploads:** Uses Retrofit's streaming-friendly multipart handling for PDFs to avoid loading entire files into memory.
- **Resource Cleanup:** The `DocRepository` ensures that `EventSource` connections are properly cancelled in the `awaitClose` block of the streaming Flow, preventing memory leaks and dangling sockets.

## 3. Memory & Resource Management
- **Image Loading:** Currently, the app uses minimal images (monochrome icons and a single logo). If user avatars are expanded, a library like Coil should be used for memory-efficient bitmap caching.
- **Coroutine Scopes:** Most operations are bound to `viewModelScope`, ensuring that background tasks (like job polling) are killed the moment the user leaves the feature.
- **Encrypted Storage:** `EncryptedSharedPreferences` has a small performance overhead compared to standard prefs, but it is necessary for security. The app minimizes impact by only reading the token on startup and during rare 401 refresh scenarios.

## 4. Heavy UI Components
- **Animations:** Animations like the blinking cursor and typing dots use `InfiniteTransition` and `graphicsLayer` (alpha/scale). Applying transformations via `graphicsLayer` is more efficient as it often offloads the work to the GPU.
- **Shimmer Effects:** The upload card shimmer effect is carefully implemented to only run while an upload is active, reducing CPU idle usage.

## 5. Potential Bottlenecks
- **JSON Parsing:** For very long chat histories, parsing large JSON arrays into DTOs and then mapping them to domain models could cause minor UI stutters. Currently, history is loaded per session to mitigate this.
- **Safety Watchdogs:** The 30-second safety timeout in `ChatViewModel` prevents the app from consuming battery by waiting indefinitely for an SSE connection that the backend might have silently dropped.
