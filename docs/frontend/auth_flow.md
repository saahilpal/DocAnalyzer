# Authentication Flow: DocAnalyzer

DocAnalyzer uses a secure JWT-based authentication system with email verification (OTP) for both registration and sensitive actions like password recovery.

## 1. Registration Flow
1. **User Input:** User enters Name, Email, and Password on the `RegisterScreen`.
2. **Backend Call:** `POST /auth/register` is triggered.
3. **Outcome:** 
    - **Success:** Backend sends an OTP to the user's email. Frontend navigates to `VerifyScreen`.
    - **Failure:** Errors (e.g., "Email already exists") are shown on the registration form.

## 2. OTP Verification Flow
1. **User Input:** User enters the 6-digit code received via email on the `VerifyScreen`.
2. **Backend Call:** `POST /auth/verify-otp`.
3. **Outcome:** 
    - **Success:** Account is marked as active. The app then automatically triggers the **Login Flow** using the credentials provided during registration.
    - **Resend:** Users can request a new code after a 30-second cooldown via `POST /auth/send-otp`.

## 3. Login Flow
1. **User Input:** Email and Password on `LoginScreen`.
2. **Backend Call:** `POST /auth/login`.
3. **Outcome:**
    - **Success:** Returns `accessToken`, `refreshToken`, and `expiresAt`.
    - **Persistence:** `AuthManager` saves these tokens in `EncryptedSharedPreferences`.
    - **Auto-Verification:** If the backend returns an `INACTIVE_ACCOUNT` error, the app automatically triggers `send-otp` and navigates the user to the `VerifyScreen`.

## 4. Token Management
- **Interception:** `AuthInterceptor` adds the `Authorization: Bearer <token>` header to all requests.
- **Expiry:** `AuthManager.isLoggedIn()` checks the `expiresAt` timestamp locally.
- **Refresh:** The `ApiService` includes a `POST /auth/refresh` endpoint for rotating tokens (implemented in the network layer to handle 401 errors seamlessly, though manual logic also exists).

## 5. Password Recovery Flow
1. **Request:** `POST /auth/request-reset` (sends OTP to email).
2. **Reset:** `POST /auth/reset-password` (takes OTP, email, and new password).

## 6. Logout Flow
1. **Action:** User clicks Logout in the Profile screen.
2. **Backend:** `DELETE /auth/session` is called to invalidate the session on the server.
3. **Local:** `AuthManager.clearSession()` removes encrypted tokens.
4. **Navigation:** UI state `isAuthenticated` becomes false, triggering a transition back to the `LoginScreen`.

## Sequence Diagram (Simplified)

```text
User -> UI: Enter Credentials
UI -> ViewModel: login()
ViewModel -> Repository: login(email, pass)
Repository -> ApiService: POST /auth/login
ApiService -> Backend: Verify Credentials
Backend -> ApiService: 200 OK + JWT Tokens
ApiService -> Repository: Success(LoginResponse)
Repository -> AuthManager: saveSession(tokens)
AuthManager -> Storage: Write Encrypted Tokens
Repository -> ViewModel: Success
ViewModel -> UI: Update State (isAuthenticated = true)
UI -> User: Navigate to Chat Screen
```
