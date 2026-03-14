# Network Layer: DocAnalyzer

The network layer is built on Retrofit and OkHttp, with specific enhancements for secure authentication and real-time data streaming.

## 1. Retrofit Configuration
Managed in `network/RetrofitClient.kt`.
- **Dynamic Base URL:** The app fetches its `baseUrl` at runtime via `RuntimeConfigManager` before initializing the Retrofit instance. This allows for flexible backend deployments without app updates.
- **Converters:** Uses `GsonConverterFactory` for JSON serialization/deserialization.
- **Interceptors:** 
    - `AuthInterceptor`: Injects the `Authorization: Bearer <token>` header.
    - `HttpLoggingInterceptor`: Used in debug builds for network profiling.

## 2. Authentication Handling
- **`AuthInterceptor`:** Checks `AuthManager` (EncryptedSharedPreferences) for an access token. If found and not expired, it adds it to every outgoing request.
- **Token Storage:** Uses `EncryptedSharedPreferences` (AES-256 GCM) to securely store JWTs locally on the device.

## 3. SSE (Server-Sent Events) Streaming
- **Library:** `com.squareup.okhttp3:okhttp-sse`.
- **Implementation:** Located in `DocRepository.streamChat(...)`.
- **Flow:** 
    1. A `POST` request is made with `Accept: text/event-stream`.
    2. An `EventSourceListener` is attached to handle `onOpen`, `onEvent`, and `onFailure`.
    3. Events like `token` (partial text), `progress` (backend status), and `done` (final response) are parsed from the raw SSE data string.
    4. These events are emitted into a Kotlin `Flow<ChatStreamEvent>`, allowing the UI to react to each piece of data as it arrives.

## 4. Error Handling (`NetworkResult`)
The app uses a sealed class `NetworkResult<T>` to standardize responses:
- **`Success(data)`:** Valid response from API.
- **`ApiError(code, message, retryable)`:** Handled backend error (e.g., 400, 429 Rate Limit).
- **`NetworkError(message)`:** Connectivity issues (UnknownHostException, Timeout).
- **`Exception(message)`:** Unexpected local failures or parsing errors.

## 5. Multipart File Uploads
- **Endpoint:** `POST /sessions/{sessionId}/pdfs`.
- **Method:** `apiService.uploadPdf(...)` uses Retrofit's `@Multipart` and `@Part` annotations.
- **Progress:** Progress is tracked not at the network level but via job polling on the backend after the initial upload is successful.
