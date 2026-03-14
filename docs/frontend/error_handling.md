# Error Handling: DocAnalyzer

The application implements a multi-layered error handling strategy to ensure a resilient user experience, especially given the unpredictable nature of AI streaming and large file uploads.

## 1. API & Network Errors
All network calls are wrapped in a `safeApiCall` block within the `BaseRepository`.
- **Sealed Result Type:** Every call returns a `NetworkResult`, which explicitly categorizes the failure:
    - **`ApiError`:** Backend returned a non-200 status or a JSON error object (e.g., 400, 429, 500).
    - **`NetworkError`:** Local connectivity issues like `UnknownHostException` (No Internet) or `SocketTimeoutException` (Server Unreachable).
    - **`Exception`:** Catch-all for unexpected local issues or parsing failures.
- **Global Error Gate:** In `MainActivity`, if the initial configuration fails to load, the user is presented with a full-screen `ErrorGate` with a "Retry" button, preventing them from entering a broken app state.

## 2. Validation Errors
- **Authentication:** `AuthViewModel` uses `ValidationUtils` to check for valid email formats and minimum password lengths before even attempting a network call. Errors are mapped to specific `emailError` or `passwordError` strings in the `AuthUiState` and displayed directly under the corresponding text fields.
- **Chat Input:** The "Send" button is disabled if the input is blank or if a message is already being sent, preventing redundant or empty requests.

## 3. SSE (Streaming) Failure Handling
Streaming errors are particularly complex as they can happen *after* a successful connection.
- **Logic Errors:** If the backend sends an `error` event over the SSE stream (e.g., "PDF_NOT_READY"), the `DocRepository` emits a `ChatStreamEvent.Error`. The `ChatViewModel` then displays a system message in the chat or a specific toast-style error.
- **Safety Timeout:** A 30-second watchdog timer starts when a message is sent. If no tokens are received for 30 seconds, the app force-stops the stream and notifies the user of a timeout, preventing the "Typing..." state from being stuck indefinitely.
- **Connection Failure:** If the SSE connection drops, the `onFailure` callback in `EventSourceListener` captures the exception and updates the UI state accordingly.

## 4. Rate Limiting (429)
- **Detection:** The backend returns a 429 status code with a "Retry-After" or specific error message.
- **UI Reaction:** The `ChatViewModel` parses the wait time and enters a "Rate Limited" state.
- **Feedback:** A countdown is shown to the user, and the send button is disabled until the rate limit expires.

## 5. File Upload Failures
- **Backend Job Errors:** If a document indexing job fails on the backend, the `JobResponse` status becomes `failed`.
- **UI Feedback:** The `UploadCard` in the chat UI turns red with a failure message, allowing the user to remove the failed upload and try again.
