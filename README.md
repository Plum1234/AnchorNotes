# AnchorNotes

AnchorNotes is a Kotlin/Java Android app for capturing rich notes with reminders, templates, tags, media attachments, and powerful search + filter tooling. It ships Room for persistence, WorkManager for background reminders, and Google Play Services location APIs for geofenced alerts.

## Requirements
- Android Studio Iguana (or newer) with Android SDK 34 installed
- JDK 17 (Gradle wrapper already targets Java 17)
- Android device or emulator running API 26+
- Google Play Services (required for geofence/location reminders)

## Getting Started
1. Clone the project  
   ```bash
   git clone <your-fork-or-clone-url> AnchorNotes
   cd AnchorNotes
   ```
2. Open the `AnchorNotes` folder in Android Studio.
3. Click **Sync Project with Gradle Files** (elephant icon).
4. Build → **Make Project** (`Cmd+F9` / `Ctrl+F9`) to ensure the project compiles.

### Running the App
1. Create or select an emulator/device:
   - API 33+ recommended so notification + exact alarm permissions are available.
   - Prefer a Google Play image so geofence APIs are present.
2. Grant runtime permissions on first launch (microphone, location, notifications, exact alarms).
3. Click **Run** (`Shift+F10`). Android Studio deploys `app/src/main` to the selected device.

### Add Sample Data (Optional but Recommended)
Pick either option before validating search/filter flows:
- **Via UI**: Tap the FAB to add 3–5 notes with varied titles, attachments, and tags.
- **Programmatically**: Temporarily add `TestDataHelper.populateTestData(this)` inside `MainActivity.onCreate()` after the `HomeFragment` transaction (see `QUICK_TEST_STEPS.md`). Remove the snippet when you are done.

## Project Structure
- `app/src/main/java/com/example/anchornotes` – Activities, fragments, view models, repositories, Room entities/DAOs, and reminder helpers.
- `app/src/main/res` – Layouts, drawables, menus, themes.
- `app/src/androidTest` – Espresso/UIAutomator “black” tests and other instrumentation suites.
- `app/src/test` – Robolectric + Mockito unit tests.
- `SEARCH_FILTER_IMPLEMENTATION.md` – Deep dive into the search/filter architecture.
- `TESTING_GUIDE.md` & `QUICK_TEST_STEPS.md` – Manual verification playbooks.

## Running Tests
Run tests from Android Studio (Gradle panel) or the command line:

| Command | Purpose |
| --- | --- |
| `./gradlew testDebugUnitTest` | JVM unit tests (Robolectric, Mockito, Room). |
| `./gradlew connectedDebugAndroidTest` | Instrumentation/Espresso suites on a connected device/emulator. |

> Tip: The “black” end-to-end scenarios live under `app/src/androidTest/black_tests`. Selectively run them from the IDE by right-clicking a class such as `CreateLocationReminderAndTriggerOnEnterTest`.

## Location & Reminder Notes
- Geofenced reminders use `com.google.android.gms:play-services-location`. Ensure Google Play Services is up to date and grant foreground/background location access.
- Exact-alarm permissions (`SCHEDULE_EXACT_ALARM`) must be manually enabled on Android 12+ if the system prompts for it.
- Voice notes rely on `RECORD_AUDIO`; test on hardware with a microphone if possible.

## Troubleshooting
- **Gradle/Kotlin daemon issues**: see `FIX_KOTLIN_DAEMON.md`.
- **Search/filter regressions**: follow `QUICK_TEST_STEPS.md` for a reproducible checklist.
- **Database resets**: Room currently uses `fallbackToDestructiveMigration()`, so schema bumps will wipe local data—this is expected during development.
- **Location testing on emulators**: use Android Studio’s Location pane to send mock coordinates that intersect with the geofence in your reminder.

## Additional Documentation
- `TESTING_GUIDE.md` – Detailed manual verification scenarios.
- `QUICK_TEST_STEPS.md` – TL;DR of how to validate the search & filter UI.
- `SEARCH_FILTER_IMPLEMENTATION.md` – Architectural reference for search/filter flows.
- `FIX_KOTLIN_DAEMON.md` – Tips for resolving stubborn Gradle sync failures.

That’s all you need to build, run, and validate AnchorNotes locally. Let me know if you need platform-specific steps or CI guidance.

