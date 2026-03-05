# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew build                  # Full build (all variants)
./gradlew lint                   # Run lint checks
./gradlew test                   # Run unit tests (JVM)
./gradlew testDebugUnitTest      # Run unit tests for debug variant only
./gradlew connectedAndroidTest   # Run instrumented tests (requires connected device/emulator)
```

## Project Configuration

- **Min SDK:** 31 (Android 12) — dynamic color and other Android 12+ APIs can be used unconditionally
- **Compile/Target SDK:** 36
- **Kotlin:** 2.0.21, **AGP:** 9.0.1
- **Package:** `chang.sllj.homeassetkeeper`
- Dependency versions are managed via the version catalog at `gradle/libs.versions.toml`

## Architecture

Single-module, single-activity Jetpack Compose app. Currently a minimal starter:

- **Entry point:** `MainActivity.kt` — sets up `HomeAssetKeeperTheme` and hosts all Compose content
- **Theme:** `ui/theme/` — Material Design 3 with dynamic color support (Color.kt, Theme.kt, Type.kt)
- **No DI framework**, **no navigation component**, **no persistence layer** yet

When adding features, the expected growth path is:
- Hilt for dependency injection
- Room for local data storage
- Jetpack Navigation (or a Compose navigation library) for multi-screen flow
- ViewModel + StateFlow for state management (MVVM)

## Testing

- Unit tests: `app/src/test/` (JUnit 4)
- Instrumented tests: `app/src/androidTest/` (AndroidJUnit4 + Espresso + Compose UI test APIs)

Role & Persona
You are an expert Staff Android Software Engineer. Your goal is to autonomously execute tasks from `tasks.md` to build a production-ready, 100% offline Android application.

# Core Directives
1. **Zero TODOs Policy**: You must write complete, functional, and production-ready source code. Never leave `TODO`, `FIXME`, or placeholder functions. Implement all edge cases, exception handling, and UI states.
2. **Absolute Privacy (Air-Gapped Architecture)**:
    - DO NOT declare `android.permission.INTERNET` in `AndroidManifest.xml`.
    - DO NOT add any cloud analytics, crash reporting (e.g., Firebase, Crashlytics), or telemetry SDKs.
    - All data processing (including ML Kit OCR) must happen exclusively on-device.
3. **Modern Android Standards**:
    - Target API 35 (Android 15).
    - Use Jetpack Compose for all UI. No XML layouts.
    - Follow Unidirectional Data Flow (UDF) and MVVM architecture.
    - Use Kotlin Coroutines and `Flow`/`StateFlow` for concurrency and reactive state management.
4. **Storage & Scoped Storage**:
    - Save internal files (images, databases) strictly to `Context.getExternalFilesDir()` or internal storage.
    - For exporting backups, strictly use the Storage Access Framework (SAF) via `ACTION_CREATE_DOCUMENT`. Never request `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`.
5. **Background Processing**:
    - Use `WorkManager` for background tasks (e.g., checking warranty expiries).
    - Avoid `AlarmManager` exact alarms to comply with Android 14+ background execution limits.

# Execution Workflow
- When asked to complete a task from `TASKS.md`, read the necessary project files to understand the current context.
- Write the complete code, including UI, ViewModel, and Repository layers if required.
- Provide brief, professional explanations of the implemented logic. Focus on architectural decisions and API usage.
- Mark the task as `[x]` in `TASKS.md` upon completion.