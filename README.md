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

The Xcode project runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` during the iOS build to produce the shared Kotlin framework. This build phase unsets `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED` so Xcode builds always refresh the shared Kotlin/iOS framework instead of using stale generated sources.

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


## Audit history and learning statistics

Room version 3 adds `audit_events`; migration from version 2 preserves existing data.
History starts at upgrade time. Events are append-only through `AuditDao`, with UTC
millisecond timestamps, unique event keys, optional profile/category/word IDs,
word and meaning snapshots, quiz session/question IDs, raw submitted answers,
correctness, attempt numbers, notification IDs, and optional JSON details.
Action names are extensible strings so new features can add events without a migration.

Profile, category, and word inserts, updates, and deletes are captured by SQLite
triggers, including dataset imports and cascaded deletes. The history write is in
the same transaction as the mutation. Update/delete details preserve old values.
Audit rows have no cascading foreign keys: history survives moving, editing, or
deleting words, categories, and profiles. Statistics attribute activity to the
category at the time of the event, not a word's current category.

All three quiz modes log session start/end, displayed/skipped questions, and each
submitted answer. A session ends when leaving the quiz or changing its profile,
category, or mode; this is open-ended practice, not a fixed-length completed test.
Retries keep the same question ID and increment `attemptNumber`. Repeated identical
submissions, blank submissions, and submissions after a correct answer are ignored.
Speech recognition errors are not incorrect answers. First-answer accuracy excludes
retries. Sessions with no available questions are not counted.

Notification outcomes are distinct:

- Android: `ALERT_ATTEMPTED`, then `ALERT_POSTED`, `ALERT_BLOCKED`, or `ALERT_FAILED`.
  Posted means the notification manager accepted the call, not that the user read it.
- iOS: `ALERT_ATTEMPTED`, then `ALERT_SCHEDULED`, `ALERT_BLOCKED`, or `ALERT_FAILED`.
  Delivery is recorded separately when observed in the foreground, on a notification
  response, or by reconciling notifications still in Notification Center at launch/resume.
  Repeated observations use the same event key and do not double-count delivery.
  Pending cancellations and notification opens are also logged.
- Sample alerts are retained in history but excluded from learning alert totals.
  Scheduled alerts are never counted as delivered. iOS notifications dismissed while
  the app was inactive may never be observable and cannot be claimed as delivered.

```kotlin
val audit = database.get().auditDao()
val general = audit.generalStatistics(profileId)
val category = audit.categoryStatistics(profileId, categoryId)
val word = audit.wordStatistics(profileId, wordId)
val daily = audit.dailyStatistics(profileId = profileId, fromInclusive = startMillis, untilExclusive = endMillis)
val recent = audit.history(profileId = profileId, limit = 100, offset = 0)
val quiz = audit.quizHistory(sessionId)
```

Statistics are observable `Flow`s. Time windows include their start and exclude
their end; daily buckets use UTC. Totals include correct/incorrect attempts,
first-answer accuracy, question counts, session counts, and separate alert outcomes.
Accuracy is null when no answers exist. `quizzesPracticed` counts distinct sessions
with answers within the requested scope, including mixed-category sessions.
`quizzesStarted`/`quizzesEnded` count lifecycle events at the selected session scope;
use `quizzesPracticed` for per-word or mixed-session category participation.
`statistics()` with no profile filter provides totals across all retained profiles.

For new non-database actions, append an `AuditEvent` with a stable event key when
retrying. `AuditTrail.record` supplies an app-lifetime ordered queue for UI events,
with storage retries and a visible quiz error state. Normal navigation does not
cancel pending writes; abrupt process termination can lose not-yet-persisted UI
events or leave a session without an end event. Notification sending persists an
attempt before the OS call. An interruption between OS acceptance and the final
history write remains an attempt with unknown outcome. Device delivery behavior
requires physical-device validation.

Verification:

```sh
./gradlew :composeApp:testDebugUnitTest
python3 -m unittest discover -s tests -v
```

The SQL tests execute production migration/trigger/query SQL against SQLite and
compare the migrated table with Room's exported version 3 schema. Kotlin tests
cover quiz retry, duplicate, scope, empty-session, and skipped-question behavior.


Home displays profile-wide reminders sent, questions answered, and first-answer
accuracy below the translator. Its selectable seven-day chart uses local-calendar
days (including daylight-saving changes), fills inactive days with zero, and shows
questions and reminders separately. `AuditDao.homeStatistics(profileId)` and
`homeActivity(profileId, nowMillis)` supply these live values. Unlike the audit API's
UTC daily export, Home uses the device time zone. Samples and scheduled/blocked
notifications are excluded, and notification IDs are deduplicated across outcomes.
