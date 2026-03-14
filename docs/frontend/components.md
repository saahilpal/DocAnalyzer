# UI Component Library: DocAnalyzer

The application uses a highly customized set of Jetpack Compose components, following a monochrome "GPT-style" design language.

## Core Chat Components

### 1. `ChatBubble`
- **Purpose:** Displays individual messages in the chat interface.
- **Features:**
    - Supports User and AI (Assistant) roles with distinct alignments and backgrounds.
    - **Streaming Support:** Implements a "typing" effect for AI responses, where text appears character-by-character.
    - **Visual Cues:** Includes a blinking cursor and a "thinking" (fading) animation for empty AI messages.
- **Animations:** Uses `animateFloatAsState` for smooth entry (fade and scale).

### 2. `TypingIndicator`
- **Purpose:** Shown when the AI is processing but hasn't started sending tokens yet.
- **Implementation:** Three animated dots with staggered `infiniteRepeatable` alpha transitions.

### 3. `ChatInputBar`
- **Purpose:** The fixed bottom bar for user interaction.
- **Elements:**
    - **Upload Button:** Triggers the system file picker for PDFs.
    - **Text Field:** Custom `BasicTextField` with a hint ("Ask Doc Analyzer").
    - **Send Button:** Circular button that scales on press and shows a `CircularProgressIndicator` while sending.

### 4. `UploadCard`
- **Purpose:** Displays the real-time progress of a PDF upload and backend indexing job.
- **States:** `UPLOADING`, `PROCESSING`, `INDEXED` (Completed), `FAILED`.
- **Visuals:**
    - Shimmer effect during upload.
    - Linear progress bar tied to backend job progress.
    - Checkmark animation on completion.
    - Destructive red styling on failure.

## Navigation & Sidebar Components

### 1. `SidebarDrawer`
- **Purpose:** The main navigation menu.
- **Features:**
    - Search bar to filter chat sessions.
    - "New Chat" button.
    - List of historical chat sessions (`SidebarItem`).
    - User profile footer with email and avatar.

### 2. `SidebarItem`
- **Purpose:** A single entry in the session list.
- **Features:**
    - Highlighted "Active" state.
    - Long-press menu for "Rename" and "Delete".
    - Inline editing mode for renaming sessions.

## Shared Infrastructure Components

### 1. `AuthTextField` & `AuthButton`
- **Purpose:** Standardized inputs and buttons for the authentication flow.
- **Features:** High-contrast borders, error state handling, and integrated loading indicators.

### 2. `DocAnalyzerLogo`
- **Purpose:** Brand representation, often used with entry animations.

### 3. `SkeletonCard`
- **Purpose:** Placeholder loading state for session lists and document viewers.
