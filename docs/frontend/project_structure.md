# Project Structure: DocAnalyzer

The repository is organized into a clean, feature-and-layer hybrid structure.

## Packages and Responsibilities

- **`com.nitrous.docanalyzer`**
  - `MainActivity.kt`: The entry point of the application. It handles initial configuration loading, splash screen transitions, and the root navigation graph.

- **`auth/`**: Encapsulates all authentication-related features.
  - `data/`: Contains `AuthApiService`, `AuthManager` (EncryptedSharedPreferences), and `AuthRepository`.
  - `ui/`: Compose screens for Login, Register, Verify, and Reset Password.
  - `viewmodel/`: `AuthViewModel` and its factory for managing auth state.

- **`network/`**: The infrastructure layer for backend communication.
  - `dto/`: Data Transfer Objects (Request/Response models) for all API endpoints.
  - `RetrofitClient.kt`: Singleton for Retrofit and OkHttp configuration.
  - `AuthInterceptor.kt`: Injects Bearer tokens into outgoing requests.
  - `NetworkResult.kt`: A sealed class for handling API success, failure, and network errors.
  - `RuntimeConfigManager.kt`: Manages dynamic configuration (e.g., base URL) from remote sources.
  - `BaseRepository.kt`: Provides helper methods for making safe API calls.

- **`ui/`**: General UI components and design system.
  - `components/`: Reusable widgets like `SkeletonCard`, `DocAnalyzerLogo`, and custom buttons.
  - `dialogs/`: Standardized dialogs for renaming or deleting sessions.
  - `motion/`: Definitions for transitions and animations using `AnimatedContent` and `Crossfade`.
  - `screens/`: Feature-specific screens like `ChatScreen`.
  - `sidebar/`: The drawer navigation component.
  - `theme/`: The design system implementation (Colors, Typography, Dimensions, Shapes).
  - `splash/`: Splash screen and launch animations.

- **`repository/`**: Domain-specific repositories.
  - `DocRepository.kt`: Handles sessions, PDF management, and chat streaming.

- **`viewmodel/`**: Feature ViewModels.
  - `ChatViewModel.kt`: The primary ViewModel for managing chat history, active sessions, file uploads, and AI streaming.

- **`model/`**: Domain models used throughout the app (e.g., `ChatMessage`, `Session`, `User`).

- **`mapper/`**: Mappers to convert between Network DTOs and Domain Models, keeping the UI layer decoupled from the API structure.

- **`util/`**: Utility classes for validation, file handling, and formatting.
