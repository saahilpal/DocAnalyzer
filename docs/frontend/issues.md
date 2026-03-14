# Problems and Potential Bugs: DocAnalyzer

During the static analysis of the DocAnalyzer codebase, the following issues, potential bugs, and architectural concerns were identified.

## 1. Architectural Concerns

### Manual Dependency Injection
- **Issue:** The project uses a manual DI approach (e.g., `RetrofitClient` as a service locator).
- **Risk:** As the app grows, this will lead to tightly coupled components and make unit testing difficult.
- **Recommendation:** Migrate to a standard DI framework like **Hilt** to manage singleton lifetimes and facilitate constructor injection in ViewModels and Repositories.

### Lack of Local Caching (Offline Support)
- **Issue:** The app relies entirely on the network. Chat history is only cached in memory within the `ChatViewModel`.
- **Risk:** If the app is killed by the OS, all history is lost until the next API call. Users cannot view previous chats without an active internet connection.
- **Recommendation:** Implement **Room Database** to persist chat sessions and messages locally.

## 2. Potential Network Bugs

### Token Refresh in SSE Requests
- **Issue:** The `AuthInterceptor` handles 401 token refreshes. However, SSE connections (`okhttp3-sse`) are persistent and might not behave correctly when the interceptor tries to transparently retry a request that has already been upgraded to a stream.
- **Risk:** A streaming request might fail silently or enter an infinite retry loop if the token expires exactly during the SSE handshake.

### Thread Blocking in Interceptor
- **Issue:** `Thread.sleep()` is used in `AuthInterceptor` for 429 Rate Limit handling.
- **Risk:** This blocks the current OkHttp thread. While acceptable for a few seconds, it can lead to thread pool exhaustion if many requests are rate-limited simultaneously.
- **Recommendation:** Use a specialized `RateLimiter` or handle retries at the Repository/ViewModel level using coroutine `delay()`.

### GlobalScope Usage
- **Issue:** `GlobalScope.launch` is used in `ChatViewModel.logout`.
- **Risk:** `GlobalScope` is generally discouraged as it can lead to memory leaks and tasks running after the app should have terminated. 
- **Recommendation:** Use a dedicated application-level `CoroutineScope` or a `WorkManager` task for non-critical cleanup like logging out from the server.

## 3. UI/UX Inconsistencies

### Hardcoded Strings
- **Issue:** Many labels and messages in the Auth flow and Chat screens are hardcoded strings or use a custom `Strings` object instead of the standard `strings.xml`.
- **Risk:** This makes localization (i.e., supporting multiple languages) much harder.
- **Recommendation:** Move all user-facing text to `res/values/strings.xml`.

### Safety Timeout vs. Long AI Responses
- **Issue:** `ChatViewModel` has a 30-second safety timeout for streaming.
- **Risk:** For extremely long or complex document analyses, the AI might take longer than 30 seconds to generate a full response, leading to a premature "Response timed out" error even if the backend is still working correctly.
- **Recommendation:** Reset the timeout timer on *every* token received, not just once at the start.

## 4. Stability & Edge Cases

### File Path Handling
- **Issue:** In `ChatScreen.kt`, temporary files are created in `cacheDir` using `System.currentTimeMillis()`.
- **Risk:** If two files are picked rapidly, there could be a naming collision. Additionally, there is no explicit cleanup of these cached files after a successful or failed upload.
- **Recommendation:** Use `File.createTempFile()` and ensure deletion in a `finally` block or via a periodic cleanup worker.

### State Reset on Logout
- **Issue:** When logging out, the `AuthViewModel` clears its local state, but the `ChatViewModel` (which is a separate singleton/factory instance) might still hold references to the previous user's sessions in its internal `_sessionMessages` map.
- **Risk:** Information leakage if a second user logs in on the same device session without the app process restarting.
- **Recommendation:** Implement a global "Clear App State" mechanism that resets all ViewModels upon logout.
