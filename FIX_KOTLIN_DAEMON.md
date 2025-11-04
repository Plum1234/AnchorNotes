# Fix: Kotlin Compile Daemon Error

## The Error
```
The daemon has terminated unexpectedly on startup attempt #1 with error code: 0
Kotlin compile daemon is ready
```

This is a common Gradle/Kotlin daemon issue. Follow these steps:

## Solution Steps

### Step 1: Stop All Gradle Daemons

**In Android Studio:**
1. Go to **File → Settings** (or **Android Studio → Preferences** on Mac)
2. Navigate to **Build, Execution, Deployment → Build Tools → Gradle**
3. Click **Stop Gradle daemons** button

**Or via Terminal:**
```bash
cd /Users/jeffreyyang/Desktop/AnchorNotes
./gradlew --stop
```

### Step 2: Clean Build Cache

**In Android Studio:**
1. **Build → Clean Project**
2. **Build → Rebuild Project**

**Or via Terminal:**
```bash
./gradlew clean
./gradlew cleanBuildCache
```

### Step 3: Invalidate Caches

**In Android Studio:**
1. **File → Invalidate Caches...**
2. Check all boxes:
   - Clear file system cache and Local History
   - Clear downloaded shared indexes
3. Click **Invalidate and Restart**

### Step 4: Check Gradle Settings

**In Android Studio:**
1. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
2. **Gradle JVM**: Should be set to a valid JDK (e.g., "17" or "Embedded JDK")
3. **Build and run using**: Should be "Gradle" (not IntelliJ)
4. **Run tests using**: Should be "Gradle" (not IntelliJ)

### Step 5: Increase Gradle Memory (if needed)

**In Android Studio:**
1. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
2. Under **Gradle options**, set:
   - **Gradle VM options**: `-Xmx2048m -XX:MaxMetaspaceSize=512m`

**Or create/edit `gradle.properties` file:**

Add/update these lines:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

### Step 6: Sync and Build Again

1. **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. **Build → Make Project**

## Alternative: Use Kotlin Plugin Directly

If the issue persists, we can configure Kotlin differently. The root `build.gradle.kts` has been updated to include the parcelize plugin.

## If Still Not Working

### Option 1: Downgrade Kotlin Version

Edit `build.gradle.kts` (root level):
```kotlin
id("org.jetbrains.kotlin.android") version "1.9.24" apply false
id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.24" apply false
```

### Option 2: Remove Parcelize Plugin Temporarily

In `app/build.gradle.kts`, remove the parcelize plugin:
```kotlin
plugins { 
    id("com.android.application")
    kotlin("android")
    // kotlin("plugin.parcelize")  // Comment out temporarily
}
```

And update `NoteSearchFilter.kt` to use `@Serializable` instead of `@Parcelize` (requires adding kotlinx-serialization).

### Option 3: Check Java Version

Make sure you're using Java 17:
- In Android Studio: **File → Project Structure → SDK Location**
- Check **JDK location** is set correctly

## Quick Fix Commands (Terminal)

Run these in order:
```bash
cd /Users/jeffreyyang/Desktop/AnchorNotes

# Stop daemons
./gradlew --stop

# Clean everything
./gradlew clean
rm -rf .gradle
rm -rf build
rm -rf app/build

# Rebuild
./gradlew build
```

## Most Common Fix

**The most common fix is Step 1 + Step 2:**
1. Stop Gradle daemons
2. Clean project
3. Sync Gradle files
4. Rebuild

Try this first before trying other solutions.

