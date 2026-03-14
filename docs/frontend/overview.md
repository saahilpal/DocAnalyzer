# App Overview: DocAnalyzer

## Purpose
DocAnalyzer is an Android application designed for intelligent document interaction. It allows users to upload PDF documents, which are then processed and indexed by a backend RAG (Retrieval-Augmented Generation) system. Users can then engage in a chat interface to ask questions and receive insights based on the uploaded documents. The app supports real-time streaming of AI responses and comprehensive session management.

## High-Level Architecture
The application follows the **MVVM (Model-View-ViewModel)** architectural pattern, ensuring a clean separation of concerns between the UI, business logic, and data sources.

- **UI Layer (Jetpack Compose):** Handles user interactions and renders the state provided by ViewModels.
- **ViewModel Layer:** Manages UI state, handles user actions, and communicates with repositories. It uses `StateFlow` to expose state to the UI.
- **Repository Layer:** Acts as a single source of truth for data, coordinating between the network layer and local models.
- **Network Layer (Retrofit/OkHttp):** Handles communication with the Node.js backend API, including authentication, multipart file uploads, and SSE (Server-Sent Events) streaming.

## Main Modules
- **`com.nitrous.docanalyzer.auth`**: Handles user authentication, including registration, login, OTP verification, and password resets.
- **`com.nitrous.docanalyzer.ui`**: Contains the main application screens (Chat), reusable UI components, and the design system (theme, typography, dimensions).
- **`com.nitrous.docanalyzer.network`**: Infrastructure for API communication, including Retrofit setup, interceptors for auth, and streaming logic.
- **`com.nitrous.docanalyzer.data/repository`**: Data handling logic for documents and chat sessions.

## Data Flow
1. **User Action:** User interacts with a Compose screen (e.g., clicks "Send Message").
2. **ViewModel:** The ViewModel receives the action and triggers a repository call.
3. **Repository:** The Repository calls the `ApiService` via Retrofit.
4. **Backend:** The Node.js API processes the request.
5. **Network Result:** The repository wraps the response in a `NetworkResult` (Success/Error/Exception).
6. **State Update:** The ViewModel updates its `StateFlow` with the new data or error message.
7. **UI Recomposition:** The UI observes the state change and automatically re-renders to reflect the current state.

## Dependencies
- **UI:** Jetpack Compose (BOM 2024.10.00), Material 3, Navigation Compose.
- **Architecture:** Lifecycle ViewModel, Compose-ViewModel integration.
- **Networking:** Retrofit 2.9.0, OkHttp 4.12.0 (with SSE support), Gson.
- **Concurrency:** Kotlin Coroutines.
- **Security:** AndroidX Security Crypto (for encrypted token storage in `AuthManager`).
- **Media:** File picking and multipart handling for PDF uploads.
