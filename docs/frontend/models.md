# Data Models: DocAnalyzer

The application maintains a clear distinction between **Network DTOs** (Data Transfer Objects) used for API communication and **Domain Models** used for UI state and business logic.

## 1. Domain Models
Located in `model/Models.kt`. These are optimized for the Android frontend and Compose rendering.

| Model | Purpose |
| :--- | :--- |
| **`User`** | Represents the authenticated user profile (ID, Name, Email, Avatar). |
| **`ChatSession`** | Represents an analysis context. Includes UI-friendly fields like `displayDate` and `pdfCountBadge`. |
| **`ChatMessage`** | Represents a single message. Includes the `role` (User/AI/System) and an ID for efficient list rendering. |
| **`UploadFile`** | Tracks the lifecycle of a document upload, including its real-time progress and status. |

## 2. API Response Models (DTOs)
Located in `network/dto/ApiModels.kt`. These match the backend Node.js API schema.

- **`ApiResponse<T>`**: The standard wrapper for all backend responses, containing an `ok` boolean, the `data` payload, and an optional `error` object.
- **`ApiError`**: Contains a `code` (e.g., `RATE_LIMITED`), a user-facing `message`, and a `retryable` flag.
- **`LoginResponse`**: Returns JWT tokens (`accessToken`, `refreshToken`) and user info.
- **`SessionDto` / `SessionMetaDto`**: Data for chat sessions, including metadata like message and PDF counts.
- **`HistoryItemDto`**: Individual messages from the session history.
- **`PdfDto`**: Metadata for uploaded files (ID, Title, Status).
- **`JobResponse`**: Status and progress of asynchronous backend tasks (like PDF indexing).

## 3. Streaming (SSE) Models
- **`ChatEvent`**: The root model for every message received over the SSE stream.
- **`ChatEventData`**: Contains the specific payload of an event, such as a single `token` (string) or a `stage` update.

## 4. UI State Models
Located within their respective ViewModels (e.g., `ChatUiState`, `AuthUiState`).
These are "Aggregator Models" that combine various domain models, loading flags, and error strings into a single object that the UI observes. This ensures a Single Source of Truth for each screen's state.

## 5. Mappers
Located in `mapper/Mappers.kt`.
The app uses extension functions (e.g., `SessionDto.toModel()`) to transform raw network data into clean domain models. This layer handles:
- **Null Safety:** Providing defaults for missing API values.
- **Data Formatting:** Converting ISO-8601 strings to relative time spans (e.g., "2 hours ago").
- **Logic Encapsulation:** Deciding the correct `MessageRole` based on string values from the backend.
