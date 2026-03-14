# Navigation: DocAnalyzer

The application uses a custom state-driven navigation system built with Jetpack Compose's `AnimatedContent` for smooth transitions, rather than the standard Jetpack Navigation Component, to allow for more granular control over transition animations and state-dependent routing.

## Navigation Graph

The root navigation is managed in `MainActivity.kt` within the `AppNavigation` composable.

### Routes (Screens)
| Screen | Route/Enum | Description |
| :--- | :--- | :--- |
| **Login** | `Screen.Login` | Main authentication entry point. |
| **Register** | `Screen.Register` | User registration. |
| **Verify** | `Screen.Verify` | OTP verification. |
| **Chat** | `Screen.Chat` | Main application hub (Authenticated). |
| **Request Reset** | `Screen.RequestReset` | Trigger password recovery email. |
| **Reset Password** | `Screen.ResetPassword` | Enter new password with OTP. |

### Transitions
- **Launch Transition:** `AppLaunchScreen` uses a `Crossfade` to transition into the main app once its animation completes.
- **Screen Transitions:** Defined in `ui/motion/AppMotion`.
    - `EnterTransition`: Slide In + Fade In.
    - `ExitTransition`: Slide Out + Fade Out.
- **Micro-navigation:** `ChatScreen` uses an internal `AppScreen` enum to toggle between `Chat` and `Profile` views without a full navigation event.

## Navigation Flow

### Authentication Flow
1. **Start:** `Login`
2. **Path A:** `Login` -> `Register` -> `Verify` -> `Login`
3. **Path B:** `Login` -> `RequestReset` -> `ResetPassword` -> `Login`
4. **Success:** `Login` or `Verify` -> `Chat`

### Authenticated Flow
1. **Active:** `Chat` (Main)
2. **Profile:** `Chat` -> `Profile` (Internal toggle)
3. **Logout:** `Chat` -> `Login`

## Parameter Passing
Parameters are primarily passed via the shared `AuthViewModel`. For instance, when navigating from `Register` to `Verify`, the user's email is already stored in the ViewModel state, so no explicit navigation arguments are needed.

## Back Stack Behavior
- `BackHandler` is used in `ChatScreen` to:
    1. Close the `ModalNavigationDrawer` if open.
    2. Navigate back from `Profile` to `Chat` view if the profile is showing.
- System back button in the Auth flow typically returns to the previous logical screen (e.g., `Register` back to `Login`).
