# Testing Guide: Search & Filter Functionality

## Quick Start Testing

### Step 1: Build and Run the App

1. **Sync Gradle Files**:
   - In Android Studio, click "Sync Project with Gradle Files" (elephant icon in toolbar)
   - Wait for sync to complete

2. **Build the Project**:
   - Build → Make Project (or `Cmd+F9` / `Ctrl+F9`)
   - Check for any compilation errors

3. **Run on Device/Emulator**:
   - Click Run button or press `Shift+F10`
   - App should launch on your device/emulator

### Step 2: Create Test Data (Optional Helper)

To test effectively, you need some notes with various properties. Here's a quick way to add test data:

**Option A: Use the App UI**
- Tap the FAB (+) button to create new notes
- Create several notes with different titles and content
- Add photos/voice to some notes (if your editor supports it)

**Option B: Add Test Data Programmatically**

Create a temporary test helper class or add this to MainActivity temporarily:

```kotlin
// In MainActivity.onCreate() - add after fragment transaction
val repo = ServiceLocator.noteRepository(this)
val now = System.currentTimeMillis()

// Create test notes
repo.createOrUpdate(null, "Shopping List", "Milk, Eggs, Bread", null, null, false)
repo.createOrUpdate(null, "Meeting Notes", "Discuss project timeline", null, null, false)
repo.createOrUpdate(null, "Recipe Ideas", "Try pasta carbonara", null, null, false)
repo.createOrUpdate(null, "Important Task", "Finish assignment", null, null, true) // pinned
```

### Step 3: Test Search Functionality

1. **Text Search**:
   - Look at the search bar at the top of the home screen
   - Type "Shopping" in the search box
   - ✅ **Expected**: Only notes containing "Shopping" should appear
   - Clear the search box
   - ✅ **Expected**: All notes should appear again

2. **Test Different Search Terms**:
   - Search for "Meeting" → should show "Meeting Notes"
   - Search for "Recipe" → should show "Recipe Ideas"
   - Search for "xyz123" → should show no results
   - ✅ **Expected**: Search works in real-time as you type

### Step 4: Test Filter Functionality

1. **Open Filter Dialog**:
   - Look for the Filter button in the toolbar (top right)
   - If you don't see it, check the overflow menu (three dots)
   - Tap the Filter button
   - ✅ **Expected**: Filter dialog opens

2. **Test Date Range Filter**:
   - In the filter dialog, tap "From Date"
   - Select a date (e.g., yesterday)
   - Tap "To Date"
   - Select today's date
   - Tap "Apply"
   - ✅ **Expected**: Only notes updated in that date range appear

3. **Test Flag Filters**:
   - Open filter dialog again
   - Check "Has Photo" (if you have notes with photos)
   - Tap "Apply"
   - ✅ **Expected**: Only notes with photos appear
   - Repeat for "Has Voice" and "Has Location"

4. **Test Tag Filter** (if you have tags):
   - Open filter dialog
   - Check one or more tags
   - Tap "Apply"
   - ✅ **Expected**: Only notes with selected tags appear

5. **Test Combined Filters**:
   - Open filter dialog
   - Check "Has Photo"
   - Select a date range
   - Tap "Apply"
   - ✅ **Expected**: Only notes that match ALL criteria appear

6. **Test Clear Filter**:
   - Open filter dialog
   - Set some filters
   - Tap "Clear"
   - ✅ **Expected**: All checkboxes/date selections cleared
   - Tap "Apply"
   - ✅ **Expected**: All notes appear (no filters active)

### Step 5: Test Search + Filter Combination

1. **Type in Search Box**: Enter "Meeting"
2. **Open Filter Dialog**: Tap Filter button
3. **Set a Filter**: Check "Has Photo" or select tags
4. **Apply Filter**: Tap "Apply"
5. ✅ **Expected**: Results show notes that match BOTH the search query AND the filter

### Step 6: Verify Real-Time Updates

1. **Start with Search**: Type "Recipe"
2. **While Search is Active**: Open filter dialog, set a filter, apply
3. ✅ **Expected**: Results update to show notes matching both search and filter
4. **Clear Search**: Delete the search text
5. ✅ **Expected**: Filter still active, but results expand to all notes matching the filter

## Troubleshooting

### App Won't Compile

**Error: "Unresolved reference: HomeFragmentKotlin"**
- Make sure you've synced Gradle files
- Check that all Kotlin files are in the correct package structure
- Verify `app/build.gradle.kts` has Kotlin plugins

**Error: "Cannot find symbol: NoteViewModel"**
- Make sure `NoteViewModel.kt` is in the correct package
- Verify ViewModel dependency is added to build.gradle

**Error: "Unresolved reference: FilterDialogFragment"**
- Check that `FilterDialogFragment.kt` exists
- Verify it's in package `com.example.anchornotes.ui`

### App Crashes on Launch

**Check Logcat for errors:**
- Database migration issues → Database version changed, data may be wiped
- Missing dependencies → Check build.gradle
- ViewModel initialization → Check ServiceLocatorExtension

### Search Not Working

1. **Check SearchView**: Is it visible in the layout?
2. **Check ViewModel**: Is it observing searchResults?
3. **Check Database**: Do you have notes in the database?
4. **Check Logcat**: Look for SQL errors or exceptions

### Filter Dialog Not Opening

1. **Check Menu**: Is the Filter button visible?
2. **Check Fragment Manager**: Verify childFragmentManager is used
3. **Check DialogFragment**: Verify FilterDialogFragment is properly set up

### No Results Showing

1. **Check Database**: Do you have notes?
2. **Check Filters**: Are filters too restrictive?
3. **Clear All**: Try clearing search and filters
4. **Check Logcat**: Look for query execution errors

## Quick Verification Checklist

- [ ] App compiles and runs without errors
- [ ] Search bar is visible at top of home screen
- [ ] Filter button appears in toolbar/menu
- [ ] Typing in search box filters results in real-time
- [ ] Filter dialog opens when clicking Filter button
- [ ] Date pickers work in filter dialog
- [ ] Checkboxes work in filter dialog
- [ ] Apply button applies filters
- [ ] Clear button clears filters
- [ ] Search + Filter work together
- [ ] Results update when search/filter changes

## Advanced Testing

### Test with Many Notes

Create 20+ notes with:
- Different titles and content
- Some with photos, some without
- Some with voice, some without
- Different update dates
- Various tags (if tag system is implemented)

Verify search performance and correctness.

### Test Edge Cases

1. **Empty Search**: Type nothing → should show all notes
2. **Empty Filter**: Apply no filters → should show all notes
3. **No Results**: Search for "nonexistent123" → should show empty list
4. **Special Characters**: Search for "test's" or "test & more"
5. **Long Text**: Search in notes with very long content

## Notes

- **Database Reset**: The database version was increased to 2. If you had existing data, it may be wiped due to `fallbackToDestructiveMigration()`. This is expected for testing.
- **Location Filter**: Currently disabled because NoteEntity doesn't have location fields yet. You'll see the checkbox but it won't filter.
- **Tag System**: Tags need to be created programmatically or through a tag management UI (not yet implemented).

