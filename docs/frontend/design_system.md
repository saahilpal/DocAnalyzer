# UI/UX Design System: DocAnalyzer

The application employs a sleek, modern, and minimalist design system, drawing heavy inspiration from high-end AI productivity tools (like ChatGPT or Anthropic's Claude).

## 1. Color Palette (Monochrome/Dark)
Defined in `ui/theme/Theme.kt`.

| Color Name | Hex Code | Purpose |
| :--- | :--- | :--- |
| **BackgroundBlack** | `#000000` | Primary app background. |
| **ElevatedSurface** | `#111111` | Secondary backgrounds, cards, and top bars. |
| **BubbleBackground** | `#1A1A1A` | User chat bubbles. |
| **InputDockColor** | `#141414` | The bottom input bar container. |
| **StrokeColor** | `#2A2A2A` | Borders, dividers, and outlines. |
| **PrimaryText** | `#EDEDED` | Main readability color (off-white). |
| **SecondaryText** | `#A0A0A0` | Subtitles, hints, and inactive icons. |
| **SuccessGreen** | `#10B981` | Completed indicators and positive status. |
| **DestructiveRed** | `#EF4444` | Errors, delete actions, and alerts. |

## 2. Typography
Defined in `ui/theme/Type.kt`.
- **Primary Font Family:** System Default (Inter-like).
- **Headlines:** Semi-bold with tight letter spacing for a compact, professional look.
- **Body:** Standard weight, optimized for readability in long-form AI responses (line height: 22.sp).
- **Labels:** Uppercase with increased letter spacing (2.sp - 4.sp) used for branding and small tags.

## 3. Spacing & Dimensions
Defined in `ui/theme/Dimens.kt`.
- **Padding Scale:** 4dp (Tiny), 8dp (Small), 16dp (Medium), 24dp (Large), 32dp (Extra Large).
- **Standard Horizontal Padding:** 16dp for lists/chat, 28dp for authentication forms.
- **Icon Sizes:** 18dp (Small/Inline), 24dp (Standard Action), 48dp+ (Large/Branding).

## 4. Shapes & Corner Radius
Defined in `ui/theme/Shapes.kt`.
- **Chat Bubbles:** 16.dp (Large) for a rounded, approachable feel.
- **Buttons/Inputs:** 12.dp (Medium) or 8.dp (Small).
- **Input Dock:** Custom "DockShape" usually with a higher radius or specific rounding.

## 5. Animations & Motion
Defined in `ui/motion/Motion.kt`.
- **Screen Transitions:** `Crossfade` for major state changes (Launch -> App). `AnimatedContent` with slide/fade for screen-to-screen navigation.
- **Micro-interactions:**
    - **Press Scaling:** Components scale to `0.98f` when pressed, providing tactile feedback.
    - **SSE Streaming:** Characters fade in or appear with a blinking cursor to simulate real-time thought.
    - **Typing Indicator:** Staggered dot animations using `InfiniteTransition`.
    - **Shimmer:** Used on `UploadCard` to indicate active processing.

## 6. Dark Mode Support
The app is **Dark Mode Exclusive**. The `DocAnalyzerTheme` forces a `darkColorScheme` regardless of system settings to maintain its "pro" aesthetic.
