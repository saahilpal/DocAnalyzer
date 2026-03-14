# Chat System: DocAnalyzer

The chat system is the core feature of the DocAnalyzer application, providing a real-time, interactive interface for document-based AI analysis.

## 1. UI Layout & Messaging
- **List View:** Uses a `LazyColumn` with `itemsIndexed` for efficient rendering of message history.
- **Message Types:**
    - **User:** Right-aligned, grey background (`BubbleBackground`).
    - **AI (Assistant):** Left-aligned, dark elevated background (`ElevatedSurface`).
    - **System:** Centered or inline notifications for status updates (e.g., "Document is still processing").

## 2. Real-time Streaming Display
- **Mechanism:** The system uses Server-Sent Events (SSE) to stream tokens from the backend.
- **Token Rendering:** As each `ChatStreamEvent.Token` is received by the `ChatViewModel`, it appends the text to the `content` of the last message in the list.
- **Visual Effects:** 
    - **Typewriter Effect:** A small delay and character-by-character display simulate a natural typing flow.
    - **Blinking Cursor:** A vertical bar animation follows the last character of a streaming message.
    - **Thinking State:** Before tokens arrive, a "thinking" animation (fading dots) is shown.

## 3. SSE Consumption Logic
Implemented in `DocRepository.streamChat`:
- **Protocol:** Uses `okhttp3-sse` to maintain a persistent HTTP connection.
- **Event Mapping:**
    - `ready`: Confirms the session is prepared for the prompt.
    - `progress`: Indicates backend processing steps (e.g., "Searching documents").
    - `token`: Provides partial answer text.
    - `done`: Signal to close the stream and finalize the UI message.
    - `error`: Bubbles backend errors (like "PDF_NOT_READY") to the UI.

## 4. Message History & Persistence
- **Loading:** When a session is selected, `GET /sessions/{id}/history` is called.
- **Mapping:** `HistoryItemDto` is converted to `ChatMessage` domain models.
- **Local Cache:** ViewModels use a `SnapshotStateList` inside a map (`_sessionMessages`) to keep messages in memory for fast switching between active sessions without re-fetching history immediately.

## 5. Session Management
- **Creation:** `POST /sessions` creates a new analysis context.
- **Renaming:** Inline editing in the sidebar or a dedicated dialog updates the session title via `PATCH /sessions/{id}`.
- **Deletion:** `DELETE /sessions/{id}` removes the entire context and history.
- **Selection:** Selecting a session from the sidebar cancels any active streaming and triggers a history load for the new context.
