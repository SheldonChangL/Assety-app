# Project: Offline Home Asset & Warranty Manager

## Phase 1: Project Setup & Architecture
- [x] **Task 1.1**: Initialize Android Studio project. Configure `build.gradle.kts` with Target SDK 36 and Min SDK 31.
- [x] **Task 1.2**: Add core dependencies: Jetpack Compose BOM, ViewModel, Room, Kotlin Coroutines, and Dagger Hilt for DI.
- [x] **Task 1.3**: Add security and offline dependencies: SQLCipher for Android, CameraX, Google ML Kit Text Recognition v2 (Bundled Model `com.google.mlkit:text-recognition`), and WorkManager.
- [x] **Task 1.4**: Configure `AndroidManifest.xml`. **Strictly ensures `<uses-permission android:name="android.permission.INTERNET"/>` is NOT declared.** Added CAMERA, POST_NOTIFICATIONS permissions and SAF `<queries>`.

## Phase 2: Database Layer (Room + SQLCipher)
- [x] **Task 2.1**: Implement database encryption using `net.zetetic:android-database-sqlcipher`. Implement key generation and secure storage via Android Keystore.
- [x] **Task 2.2**: Create Entities (`ItemEntity`, `SpecificationEntity`, `WarrantyReceiptEntity`, `MaintenanceLogEntity`) with proper foreign keys and indices.
- [x] **Task 2.3**: Create DAOs using Kotlin Coroutines `Flow` for reactive, asynchronous relational queries.
- [x] **Task 2.4**: Setup `AppDatabase` and TypeConverters (for UUID, Date/Long, and Enums).

## Phase 3: Domain Logic & Repository Layer
- [x] **Task 3.1**: Implement `ItemRepository` to encapsulate CRUD operations across all related entities.
- [x] **Task 3.2**: Implement warranty expiration calculation logic (pure functions returning precise Unix Timestamps based on purchase date and warranty duration).
- [x] **Task 3.3**: Create ViewModels (`HomeViewModel`, `ItemDetailViewModel`, `FormViewModel`) to map Repository data into Compose-observable `StateFlow` using Unidirectional Data Flow (UDF).

## Phase 4: Camera & ML Kit OCR Module
- [x] **Task 4.1**: Implement `ImageCaptureManager` using CameraX to capture high-res images and save them to the App-Specific internal storage (`Context.getExternalFilesDir()`).
- [x] **Task 4.2**: Integrate ML Kit Bundled OCR. Create `OcrProcessor` to accept a Bitmap, execute asynchronous text extraction via `TextRecognizer`, and return the raw text.
- [x] **Task 4.3**: Implement Regex-based parsers to automatically extract dates and currency values from the raw OCR text.

## Phase 5: Offline Backup & Export Module (SAF)
- [x] **Task 5.1**: Implement JSON serializer to map SQLite relational data into a structured JSON string.
- [x] **Task 5.2**: Implement CSV flatter to convert asset and spec lists into comma-separated text.
- [x] **Task 5.3**: Implement ZIP packaging service to compress the JSON file and all private receipt images into a single `.zip` archive.
- [x] **Task 5.4**: Integrate Storage Access Framework (SAF). Use `ACTION_CREATE_DOCUMENT` Intent for exporting files to user-selected external directories. Do not request `READ/WRITE_EXTERNAL_STORAGE` permissions.

## Phase 6: Background Tasks & Local Notifications
- [x] **Task 6.1**: Create Notification Channels with high priority for "Warranty Expiry" and "Maintenance Due".
- [x] **Task 6.2**: Implement `MaintenanceWorker` (extending `CoroutineWorker`) to query items expiring or due within the next 30 days.
- [x] **Task 6.3**: Register WorkManager for daily execution. Trigger Local Notifications upon condition matches. Strictly avoid `AlarmManager`'s `SCHEDULE_EXACT_ALARM`.

## Phase 7: Jetpack Compose UI
- [x] **Task 7.1**: Implement Material Design 3 Theme (Typography, Colors, Shapes).
- [x] **Task 7.2**: Build Dashboard UI (upcoming expirations, maintenance cards, category overview).
- [x] **Task 7.3**: Build Asset List UI with dynamic filtering (by Location, Category).
- [x] **Task 7.4**: Build Add/Edit Item Form UI. Support dynamic key-value inputs for Specifications, and integrate Camera preview and OCR result validation.
- [x] **Task 7.5**: Build Settings & Export UI to trigger JSON/CSV/ZIP backup flows via SAF.
