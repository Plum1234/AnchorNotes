# AnchorNotes - Sprint Update README
## Overview
This document outlines the new features and improvements added to AnchorNotes during our final sprint. All changes focus on improving usability, UI polish, and core note management functionality.

## Feature Updates
Feature 1: Note Editor UI Improvements
1. Decluttered Top Toolbar
What Changed: The note editor toolbar has been streamlined to show only essential actions, with secondary functions moved to an overflow menu.
How to Access:

Open any note in the editor
The top toolbar now displays only: Back, Pin, Save, and Reminder buttons
Tap the three-dot overflow menu (⋮) in the top-right to access additional options
Formatting tools (bold, italic, checklist) are now in a separate bottom toolbar above the keyboard

2. Aligned Attachment Buttons
What Changed: "Add photo" and "Add location" buttons are now properly aligned with consistent spacing and sizing.

How to Access:
Open the note editor
Scroll to the attachment section below the text area
Both buttons now appear side-by-side with equal dimensions and clear labels

3. Delete Notes Flow
What Changed: Users can now delete notes directly from the editor or home screen.

How to Access:
From Editor:
Open any note
Tap the overflow menu (⋮) in the top-right
Select "Delete"
Confirm deletion in the dialog
You'll be returned to the home screen

From Home Screen (Optional):
Long-press on any note card
Select "Delete" from the context menu
Confirm deletion

## Feature 2: Tag Management
Tag Viewing and Direct Removal
What Changed: Tags now display as interactive chips with instant add/remove functionality.

How to Access:
Open a note in the editor
Tags appear as chips in a ChipGroup (top or bottom of editor)
To remove a tag: Tap the "X" icon on any chip
To add tags:

Tap the "Add tag" chip/button
Select tags from the multi-select dialog
Checked tags are attached; unchecked tags are removed

Tags also display as read-only chips on home screen note cards

## Feature 4: Custom Location Selection
Add Locations Beyond Current Location
What Changed: Users can now attach any location to notes, not just their current GPS position.

How to Access:
Open the note editor
Tap "Add location" in the attachment section
Choose between:

Map Picker: Pan and zoom the map, place a marker at your desired location
Search Bar: Enter an address or place name using the Places API/Geocoder

Confirm your selection
The location is saved and can be viewed via "View location"
Custom locations work with geofence reminders

## Feature 5: Template-Location Association
Associate Templates with Locations
What Changed: Templates can now be linked to specific places and automatically recommended based on your location.

## How to Access:
## Part 1: Link Template to Location

Open Template Manager
Create a new template or edit an existing one
Find the "Associate with a place (optional)" section
Use the location picker (same as Feature 4) to select a place
Save the template
Templates with locations display a "📍 [place label]" indicator in the template list

## Part 2: Location-Based Template Recommendations

Be at or near a template's associated location (or simulate in emulator)
Tap "New Note" button
Open the Template Picker
Templates within 200-300m appear in a "Recommended templates for here" section at the top
Other templates appear below in default order
If location permission is unavailable, templates display in default order

## Technical Implementation Notes
Tag Management: Uses removeTagFromNote() in NoteRepository for real-time updates
Note Deletion: Implemented deleteNote(noteId) in NoteRepository with RecyclerView auto-refresh
Custom Locations: Integrates Places API/Geocoder with geofence reminder system
Template Sorting: Proximity-based algorithm calculates distance and marks templates as "nearby"
UI Components: ChipGroup for tags, ConstraintLayout/LinearLayout for button alignment, AlertDialog for confirmations

Known Limitations
The following features were planned but not completed in this sprint:

Geofence reminder triggering on location entry
Geofence reminder retirement on location exit
Timed reminder auto-retirement after firing

# AnchorNotes - PreUpdate Draft

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

### Automatic Reminder Retirement (NEW)
Time-based reminders now **automatically retire** after their expiration period:
- **Demo Mode (Current)**: Reminders expire **20 seconds** after triggering for quick demonstration
- **Production Mode**: Change expiration to **1 hour** by editing `NoteRepository.java:342` (change `20000L` to `3600000L`)
- **How it works**: Notes appear in "Relevant Notes" section when triggered, then automatically disappear after expiration
- **Cleanup**: Runs on app startup, resume, and every 15 minutes in background via WorkManager
- **Quick Demo**: Set a reminder 1 minute from now → Wait for it to fire → Wait 20 seconds → Switch away/back to app → Note disappears!

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

