# Quick Testing Steps - Search & Filter

## Step 1: Sync & Build

1. **Sync Gradle**: Click "Sync Project with Gradle Files" (elephant icon)
2. **Build**: Build → Make Project (`Cmd+F9` / `Ctrl+F9`)
3. **Fix any errors**: Check Build output tab

## Step 2: Add Test Data (Choose One Method)

### Method A: Use Test Helper (Easiest)

Add this to `MainActivity.onCreate()` temporarily (after fragment transaction):

```kotlin
// Add test data
TestDataHelper.populateTestData(this)
```

Then remove it after testing.

### Method B: Use App UI

- Tap the FAB (+) button
- Create 3-5 notes with different titles and content
- Some with photos, some without

## Step 3: Run the App

- Click Run button or press `Shift+F10`
- App should launch

## Step 4: Test Search

1. **See the search bar** at the top
2. **Type "Shopping"** → Should show only notes with "Shopping"
3. **Type "Meeting"** → Should show only notes with "Meeting"
4. **Clear search** → Should show all notes

## Step 5: Test Filter

1. **Tap Filter button** (top right, or in menu)
2. **Filter dialog opens**
3. **Check "Has Photo"** → Tap Apply → Should show only notes with photos
4. **Clear filters** → Tap Clear → Tap Apply → Should show all notes
5. **Select tags** (if any) → Tap Apply → Should show filtered notes

## Step 6: Test Combined

1. **Type "Work" in search**
2. **Open filter, check "Has Photo"**
3. **Tap Apply**
4. **Result**: Should show notes that have "Work" AND have photos

## What You Should See

✅ **Search bar visible** at top of home screen  
✅ **Filter button** in toolbar (top right)  
✅ **Real-time search** as you type  
✅ **Filter dialog** opens with tags, dates, checkboxes  
✅ **Results update** when you apply filters  
✅ **Search + Filter work together**

## If Something Doesn't Work

1. **Check Logcat** (bottom panel) for errors
2. **Verify MainActivity** uses `HomeFragmentKotlin.newInstance()`
3. **Sync Gradle** again
4. **Clean & Rebuild**: Build → Clean Project, then Build → Rebuild Project

## Quick Checklist

- [ ] App compiles without errors
- [ ] App runs on device/emulator
- [ ] Search bar visible
- [ ] Filter button visible
- [ ] Search works (typing filters results)
- [ ] Filter dialog opens
- [ ] Filters apply correctly
- [ ] Search + Filter work together

That's it! The functionality should be working now.

