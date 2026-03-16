# Assety

Private, offline-first home asset tracking for Android.

Assety helps you catalog appliances, electronics, tools, furniture, and other household items without sending any data to the cloud. It stores records locally, keeps the database encrypted, supports photo documentation, tracks warranties and maintenance, and now includes guided on-device OCR for scanning brand and purchase date fields.

## Why This App Exists

Most home inventory apps assume cloud sync, accounts, and remote storage. Assety takes the opposite approach:

- No internet permission
- No account system
- No analytics or crash-reporting SDKs
- Local-only storage for photos and structured data
- AES-256 encrypted database using SQLCipher + Android Keystore

## Features

- Asset inventory with categories, locations, notes, and photo attachments
- Warranty tracking with expiry reminders
- Maintenance logs and periodic local notifications
- Backup and restore through Android's Storage Access Framework
- Guided camera capture using CameraX
- On-device OCR with ML Kit for:
  - brand scanning
  - purchase date scanning
- Multi-language OCR pipeline with Latin, Chinese, Japanese, Korean, and Devanagari recognizers
- Candidate confirmation flow so OCR suggestions are reviewed before being written into the form
- Tap-to-focus camera interaction for guided scans

## Privacy Model

Assety is intentionally designed as a local-first app:

- `android.permission.INTERNET` is not declared
- Captured images stay in app-controlled storage
- Records are stored in an encrypted local database
- Backup files are exported only to locations explicitly chosen by the user

See [AndroidManifest.xml](./app/src/main/AndroidManifest.xml) for the app permission model.

## Screens

Example project assets are available in [`sotre_assets/`](./sotre_assets):

- [`add_new_one.png`](./sotre_assets/add_new_one.png)
- [`one_asset.png`](./sotre_assets/one_asset.png)
- [`detail.png`](./sotre_assets/detail.png)

## Tech Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- Hilt
- Room
- SQLCipher
- WorkManager
- CameraX
- Google ML Kit Text Recognition
- Coil

## Requirements

- Android Studio with Android SDK 35/36 support
- JDK 17
- Android device or emulator running Android 12+ (`minSdk = 31`)

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle.
4. Run the `app` configuration on a device or emulator.

### Command Line Build

```bash
./gradlew :app:assembleDebug
```

### Kotlin Compile Check

```bash
./gradlew :app:compileDebugKotlin
```

## Release Signing

Release signing values are read from `local.properties`.

Expected keys:

```properties
signing.storeFile=/path/to/release.keystore
signing.storePassword=...
signing.keyAlias=...
signing.keyPassword=...
```

If these are missing, debug builds still configure normally.

## Project Structure

```text
app/src/main/java/chang/sllj/homeassetkeeper/
  backup/         Backup/export logic
  camera/         CameraX, OCR, guided scanning
  data/           Room entities, DAOs, repositories, security
  di/             Hilt modules
  notification/   Local notification helpers
  ui/             Compose screens, navigation, theme
  worker/         WorkManager jobs and scheduling
```

## OCR Notes

The guided scan flow is optimized for short, high-signal text regions rather than full-image OCR:

- users are prompted to align important text inside a bounded scan area
- scan captures are cropped before OCR runs
- OCR results are converted into ranked candidates
- the form shows candidates for confirmation instead of auto-filling blindly

This makes the feature more reliable for receipts, labels, and product nameplates, but logo-only captures can still be weaker than plain text captures.

## Roadmap

- Improve brand matching with a larger dictionary and domain-specific aliases
- Tune scan-frame positioning for real-world receipts and product labels
- Add more instrumentation and UI tests
- Continue refining the offline backup and restore flow

## License

No license file is currently included in this repository.
