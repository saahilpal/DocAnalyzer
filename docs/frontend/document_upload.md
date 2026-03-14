# Document Management: DocAnalyzer

The document management system allows users to provide context to the AI by uploading PDF files within specific chat sessions.

## 1. Document Upload UI
- **Trigger:** An "Add" (+) button in the `ChatInputBar`.
- **Picker:** Uses `ActivityResultContracts.GetContent()` restricted to `application/pdf`.
- **Preview:** While uploading, a `UploadCard` appears at the bottom of the screen, above the input bar.

## 2. Supported File Types
- **Format:** PDF only (`application/pdf`).
- **Processing:** Files are first uploaded to the backend, which then triggers an asynchronous indexing job.

## 3. Upload & Indexing Flow
1. **File Selection:** User selects a PDF.
2. **Local Copy:** The app copies the file to its internal cache to ensure stable access during upload.
3. **Multipart Upload:** `DocRepository.uploadPdf` sends the file via a multipart request.
4. **Job Polling:** The backend returns a `jobId`. The `ChatViewModel` then calls `pollJob(jobId)`.
5. **Progress Feedback:** 
    - The `DocRepository` emits progress updates from the polling flow.
    - `ChatViewModel` updates the `UploadFile` state (progress %, status).
    - `UploadCard` reflects these changes (shimmer -> progress bar -> checkmark).

## 4. Document List & Deletion
- **View Docs:** Accessible via the "View Documents" option in the `ChatMenu` (top bar).
- **UI Component:** `DocViewerSheet` (a Modal Bottom Sheet).
- **Document Status:** Displays whether each document is `indexed`, `processing`, or `failed`.
- **Deletion:**
    1. User clicks the delete icon in the document list.
    2. Frontend calls `DELETE /pdfs/{pdfId}`.
    3. On success, the document list is refreshed.

## 5. Technical Implementation Details
- **Polling Logic:** Uses a Kotlin `flow` with a 1.5-second delay between `GET /jobs/{jobId}` calls.
- **Cleanup:** Temporary files in the cache are managed by the OS/App lifecycle, but the app also clears its internal `currentUploads` list once a job is `completed`.
- **Error Handling:** If an indexing job fails, the `UploadCard` turns red and displays "Upload failed", and the error is bubbled up to the main UI error state.
