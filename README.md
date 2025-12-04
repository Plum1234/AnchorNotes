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
  - `data/db/` – Room database entities (`TemplateEntity`, `NoteEntity`, etc.) and DAOs
  - `data/repo/` – Repository layer (`TemplateRepository`, `NoteRepository`)
  - `ui/` – Fragments and dialogs (`EditTemplateDialog`, `TemplateManagerFragment`, `HomeFragment`)
  - `viewmodel/` – ViewModels (`TemplateViewModel`, `NoteViewModel`)
  - `util/` – Utility classes (`LocationUtils` for distance calculations)
  - `model/` – Data models (`PlaceSelection`, `TemplateWithProximity`)
  - `receiver/` – Broadcast receivers (`GeofenceReceiver` for geofence events)
  - `context/` – Managers (`GeofenceManager`, `ReminderManager`)
- `app/src/main/res` – Layouts, drawables, menus, themes.
- `app/src/androidTest` – Espresso/UIAutomator "black" tests and other instrumentation suites.
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

### Geofence Behavior
- **ENTER**: When entering a geofenced area, the app displays a notification and adds the note to the "Relevant Notes" section on the home screen. The note remains visible until you exit the geofence.
- **EXIT**: When leaving a geofenced area, the note is silently removed from "Relevant Notes" without showing a notification.
- The "Relevant Notes" section updates in real-time via LiveData observers in `HomeFragment`.

### Template Location Association (Feature 5)
Templates can now be associated with specific locations:
- **Create/Edit Templates**: Open Template Manager → Create/Edit Template → tap "📍 Use Current Location" to associate the template with your current GPS coordinates. The location is saved with a 175-meter radius by default.
- **Location Display**: Templates with associated locations show a "📍 [location label]" badge in the template list.
- **Clear Location**: Tap "Clear Location" in the template editor to remove the location association.
- **Proximity Sorting** (partially implemented): The infrastructure exists for sorting templates by proximity when creating notes, but the UI for "Recommended templates for here" is not yet fully connected in the template picker.

## Troubleshooting
- **Gradle/Kotlin daemon issues**: see `FIX_KOTLIN_DAEMON.md`.
- **Search/filter regressions**: follow `QUICK_TEST_STEPS.md` for a reproducible checklist.
- **Database migrations**: The app uses Room migrations to preserve data across schema changes. Current version is v5 (added template location fields). If you encounter database issues, uninstall and reinstall the app to reset the database.
- **Location testing on emulators**: use Android Studio's Location pane (Extended Controls → Location) to send mock coordinates that intersect with the geofence in your reminder. For template location testing, set your mock location before tapping "Use Current Location" in the template editor.

## Additional Documentation
- `TESTING_GUIDE.md` – Detailed manual verification scenarios.
- `QUICK_TEST_STEPS.md` – TL;DR of how to validate the search & filter UI.
- `SEARCH_FILTER_IMPLEMENTATION.md` – Architectural reference for search/filter flows.
- `FIX_KOTLIN_DAEMON.md` – Tips for resolving stubborn Gradle sync failures.

That’s all you need to build, run, and validate AnchorNotes locally. Let me know if you need platform-specific steps or CI guidance.

