# Screen Inventory: DocAnalyzer

## 1. Login Screen
- **Path:** `app/src/main/java/com/nitrous/docanalyzer/auth/ui/LoginScreen.kt`
- **Purpose:** Primary entry point for existing users to authenticate.
- **UI Elements:**
    - App Logo (`DocAnalyzerLogo`)
    - Email Input (`AuthTextField`)
    - Password Input (`AuthTextField` with secure entry)
    - "Forgot Password" link
    - "Continue" Button (`AuthButton` with loading state)
    - "Sign Up" navigation link
- **State Variables:** `email`, `password`, `isLoading`, `error`, `isSubmitted`.
- **ViewModel:** `AuthViewModel`
- **Backend calls:** `POST /auth/login`
- **Navigation entry point:** App Start (if not authenticated).

## 2. Register Screen
- **Path:** `app/src/main/java/com/nitrous/docanalyzer/auth/ui/RegisterScreen.kt`
- **Purpose:** Allows new users to create an account.
- **UI Elements:**
    - Email/Password inputs
    - Name input
    - "Create Account" button
- **ViewModel:** `AuthViewModel`
- **Backend calls:** `POST /auth/register`
- **Navigation entry point:** Link from Login Screen.

## 3. Verify Screen (OTP)
- **Path:** `app/src/main/java/com/nitrous/docanalyzer/auth/ui/VerifyScreen.kt`
- **Purpose:** Verification of user email via OTP code.
- **UI Elements:**
    - OTP digit input
    - "Verify" button
    - "Resend Code" timer/button
- **ViewModel:** `AuthViewModel`
- **Backend calls:** `POST /auth/verify-otp`, `POST /auth/send-otp`
- **Navigation entry point:** Successful registration.

## 4. Chat Screen
- **Path:** `app/src/main/java/com/nitrous/docanalyzer/ui/screens/ChatScreen.kt`
- **Purpose:** The main functional hub for document interaction and AI chat.
- **UI Elements:**
    - Navigation Drawer (Sessions list, Profile access)
    - Top Bar (App name, Session Menu: Rename/Delete/View Docs)
    - Chat Bubble List (User vs AI messages)
    - Typing Indicator (Shows while AI is processing)
    - Document Upload Progress Cards
    - Message Input Bar (Text input, PDF upload button, Send button)
- **State Variables:** `activeSessionId`, `messages`, `isTyping`, `isSending`, `currentUploads`.
- **ViewModel:** `ChatViewModel`
- **Backend calls:** 
    - `GET /sessions` (Load list)
    - `POST /sessions` (Create new)
    - `GET /sessions/{id}/history` (Load history)
    - `POST /sessions/{id}/chat?stream=true` (Streaming SSE)
    - `POST /sessions/{id}/pdfs` (Multipart upload)
- **Navigation entry point:** App Start (if authenticated) or successful Login.

## 5. Profile Screen
- **Path:** `app/src/main/java/com/nitrous/docanalyzer/ui/profile/ProfileScreen.kt`
- **Purpose:** User account management.
- **UI Elements:**
    - User Avatar/Email display
    - Change Password button
    - Logout button
- **ViewModel:** `ChatViewModel` (shares user profile state)
- **Backend calls:** `GET /auth/me`, `DELETE /auth/session` (Logout).
- **Navigation entry point:** Drawer menu.

## 6. Request Reset / Reset Password Screens
- **Paths:** 
    - `app/src/main/java/com/nitrous/docanalyzer/auth/ui/RequestResetScreen.kt`
    - `app/src/main/java/com/nitrous/docanalyzer/auth/ui/ResetPasswordScreen.kt`
- **Purpose:** Password recovery flow.
- **ViewModel:** `AuthViewModel`
- **Backend calls:** `POST /auth/request-reset`, `POST /auth/reset-password`.
