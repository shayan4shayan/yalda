# Yalda

Yalda is a Kotlin Multiplatform vocabulary app for building, importing, reviewing, and practicing language word libraries on Android and iOS.

The shared UI is built with Compose Multiplatform. Local data is stored with Room and bundled SQLite, predefined word categories are imported from a public GitHub dataset, and offline translation uses Google ML Kit for the currently configured English-to-Persian model.

## Features

- Profiles with source and target languages.
- Category and word management.
- Search across words, meanings, descriptions, and categories.
- Multiple choice, spelling, and speaking quizzes.
- Daily word reminders with exact-time or time-range schedules.
- Predefined category import from `yalda-public-dataset`.
- Offline translation model management for English to Persian.
- Android and iOS speech recognition and text-to-speech support.

## Tech Stack

- Kotlin Multiplatform `2.2.10`
- Compose Multiplatform `1.8.2`
- Android Gradle Plugin `8.13.2`
- Room `2.8.4` with bundled SQLite
- Ktor `3.3.3`
- Kotlinx Serialization
- Android WorkManager
- Google ML Kit Translate
- SwiftUI host app for iOS
- CocoaPods for iOS ML Kit dependencies

## Project Structure

```text
.
+-- composeApp/
|   +-- src/commonMain/      Shared Compose UI, view models, database, networking, and abstractions
|   +-- src/androidMain/     Android entry point and platform implementations
|   +-- src/iosMain/         iOS entry point and platform implementations
|   +-- schemas/             Exported Room schema history
+-- iosApp/
|   +-- iosApp/              SwiftUI app host and ML Kit bridge
|   +-- Podfile              iOS CocoaPods dependencies
|   +-- iosApp.xcodeproj/    Xcode project
+-- gradle/libs.versions.toml
+-- settings.gradle.kts
+-- build.gradle.kts
```

## Prerequisites

- JDK 17 or newer.
- Android Studio with the Android SDK.
- Xcode for iOS development.
- CocoaPods for iOS dependencies.

For iOS, use the workspace after installing pods:

```shell
pod install --project-directory=iosApp
open iosApp/iosApp.xcworkspace
```

## Build and Run

Build the Android debug app:

```shell
./gradlew :composeApp:assembleDebug
```

Install the Android debug app on a connected device or emulator:

```shell
./gradlew :composeApp:installDebug
```

Run iOS from Xcode:

1. Install pods with `pod install --project-directory=iosApp`.
2. Open `iosApp/iosApp.xcworkspace`.
3. Select the `iosApp` scheme and a simulator or device.
4. Build and run.

The Xcode project runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` during the iOS build to produce the shared Kotlin framework.

## Tests and Checks

Run common project checks:

```shell
./gradlew :composeApp:check
```

Run Android unit tests:

```shell
./gradlew :composeApp:testDebugUnitTest
```

## Data and Services

Yalda stores profiles, categories, and word entries in a local Room database. Category transfer data is represented as JSON with this shape:

```json
{
  "categoryName": "Example",
  "words": [
    {
      "word": "hello",
      "definition": "a greeting",
      "description": "optional note"
    }
  ]
}
```

The import screen fetches predefined category metadata from:

```text
https://raw.githubusercontent.com/shayan4shayan/yalda-public-dataset/refs/heads/main/files.index.json
```

Offline translation currently ships one catalog entry:

```text
ML Kit English to Persian
id: mlkit_en_fa
source: en
target: fa
approximate size: 30 MB
```

## Platform Notes

Android permissions:

- `INTERNET` for dataset imports and ML Kit model downloads.
- `RECORD_AUDIO` for speaking quizzes.
- `POST_NOTIFICATIONS` for daily word reminders on Android 13 and newer.

iOS permissions:

- Microphone access for speaking quizzes.
- Speech recognition access for spoken answers.
- Notification access for daily word reminders.

On iOS, CocoaPods provides `GoogleMLKit/Translate`, and the Swift bridge in `iosApp/iosApp/IOSMlKitTranslationBridge.swift` connects ML Kit to the shared Kotlin translation service.

## Development Notes

- Add shared features under `composeApp/src/commonMain`.
- Add platform-specific implementations under `androidMain` or `iosMain`.
- Keep Room schema exports in `composeApp/schemas`.
- Use `gradle/libs.versions.toml` for dependency and plugin versions.
- After changing iOS CocoaPods dependencies, rerun `pod install --project-directory=iosApp`.
