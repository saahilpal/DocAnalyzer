# DocAnalyzer

## Overview
DocAnalyzer is an Android application for uploading PDF documents and chatting with their contents through an AI-backed interface.

## Features
- Upload and index PDF documents
- Chat with indexed document content
- Streaming response experience
- Session-oriented conversation flow

## Screenshots
- Placeholder: Home screen
- Placeholder: Chat screen
- Placeholder: Upload flow

## Tech Stack
- Kotlin
- Jetpack Compose
- MVVM
- Coroutines
- Retrofit

## Architecture
The project follows an MVVM architecture with clear separation of concerns:
- UI layer: Compose screens and reusable UI components
- Presentation layer: ViewModels handling state and user actions
- Data layer: Repository, mappers, and network services
- Networking: Retrofit client and API service contracts

## Build Instructions
1. Install Android Studio (latest stable).
2. Open this project folder in Android Studio.
3. Ensure Android SDK and required build tools are installed.
4. Sync Gradle dependencies.
5. Run the `app` module on an emulator or physical Android device.

## Project Structure
```text
DocAnalyzer/
├── app/
│   ├── src/main/java/com/nitrous/docanalyzer/
│   │   ├── network/
│   │   ├── repository/
│   │   ├── ui/
│   │   └── viewmodel/
│   └── src/main/res/
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## Release Build
1. In Android Studio, select **Build > Generate Signed Bundle / APK**.
2. Choose **APK** and complete keystore/signing configuration.
3. Generate the release artifact.
4. Locate output under `app/build/outputs/apk/release/`.

## Notes
- Keep machine-specific files like `local.properties` out of version control.
- Keep build artifacts and IDE metadata ignored to maintain a clean repository.
