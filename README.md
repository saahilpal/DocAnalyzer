# DocAnalyzer

## Overview
DocAnalyzer is an Android client for document-grounded AI chat.
Users create chat sessions, upload PDF files, wait for indexing to complete, and then ask questions against indexed content. Responses are streamed token-by-token from the backend for a real-time chat experience.

## Features
- Session management with chat history drawer (create, select, delete sessions)
- PDF upload via Android file picker (`application/pdf`)
- Upload/indexing progress states: queued, uploading, processing, indexed, failed
- Guardrails that block chat until at least one PDF is fully indexed
- Streaming assistant responses using Server-Sent Events (SSE)
- Keyboard-aware Compose chat input and empty-state quick prompts
- Network/error state handling for API failures and rate limits

## Screenshots
- Placeholder: Chat screen with empty state
- Placeholder: Chat screen with uploaded PDF and streaming response
- Placeholder: Session drawer and history list

## Tech Stack
- Kotlin (1.9.23)
- Jetpack Compose + Material 3
- MVVM (StateFlow + ViewModel)
- Kotlin Coroutines
- Retrofit + OkHttp (+ OkHttp SSE)
- Gson for JSON serialization

## Architecture
The app follows a clean MVVM-style structure:

- UI Layer (`ui/screens`, `ui/components`)
  - Compose screens and reusable components
  - Renders immutable `ChatUiState`
- Presentation Layer (`viewmodel/ChatViewModel.kt`)
  - Owns UI state with `StateFlow`
  - Coordinates sessions, messages, uploads, and streaming events
- Data Layer (`repository/DocRepository.kt`)
  - Handles REST endpoints and SSE stream connection
  - Returns typed `NetworkResult` responses
- Network Layer (`network/*`)
  - Retrofit API definitions
  - Shared safe API call/error parsing logic
- Mapping Layer (`mapper/Mappers.kt`)
  - DTO-to-domain model transformations

Data flow:

```text
Compose UI -> ChatViewModel -> DocRepository -> Retrofit/OkHttp -> Backend API
```

## Backend API Requirements
The app expects a backend that provides these routes under `BASE_URL`:

- `GET /health`, `GET /ping`
- `GET/POST/DELETE /sessions`
- `GET /sessions/{sessionId}/pdfs`, `POST /sessions/{sessionId}/pdfs`
- `POST /sessions/{sessionId}/chat?stream=true` (SSE events: `token`, `done`)
- `GET /jobs/{jobId}`
- `GET/DELETE /sessions/{sessionId}/history`

## Build Instructions
1. Install Android Studio (latest stable) and JDK 17.
2. Open this project in Android Studio.
3. Ensure Android SDK 34 is installed.
4. Configure backend base URL in `app/src/main/java/com/nitrous/docanalyzer/network/RetrofitClient.kt`.
5. Sync Gradle and run the `app` module.

CLI build (optional):

```bash
./gradlew :app:assembleDebug
```

## Project Structure
```text
DocAnalyzer/
├── app/
│   ├── src/main/java/com/nitrous/docanalyzer/
│   │   ├── mapper/
│   │   ├── model/
│   │   ├── network/
│   │   ├── repository/
│   │   ├── ui/
│   │   └── viewmodel/
│   └── src/main/res/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Release Build
Generate release APK from Android Studio or CLI:

```bash
./gradlew :app:assembleRelease
```

Release output:

```text
app/build/outputs/apk/release/
```

## Notes
- Current manifest enables cleartext traffic (`android:usesCleartextTraffic="true"`) for local/dev backend use.
- For production, use HTTPS endpoints and production-safe network/security configuration.
- Session data and chat history are backend-driven; there is no local database persistence in this client.
