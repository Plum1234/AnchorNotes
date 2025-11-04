# Feature 3: Fast Search & Filtering - Implementation Summary

## Overview
This document describes the implementation of Feature 3: Fast Search & Filtering for the AnchorNotes Android app. The implementation follows MVVM architecture with Room database and Fragment-based UI.

## Files Created/Modified

### Data Models
1. **NoteSearchFilter.kt** (`com.example.anchornotes.data`)
   - Parcelable data class for search filters
   - Supports query text, tag IDs, date range, and flags (photo, voice, location)

2. **TagEntity.kt** (`com.example.anchornotes.data.db`)
   - Room entity for tags table
   - Fields: id, name

3. **NoteTagCrossRef.kt** (`com.example.anchornotes.data.db`)
   - Room entity for many-to-many relationship between notes and tags
   - Foreign keys with CASCADE delete

4. **NoteWithTags.kt** (`com.example.anchornotes.data.db`)
   - Data class combining NoteEntity with its tags using Room @Relation
   - Currently unused but prepared for future tag display features

### Database Layer
5. **NoteSearchDao.kt** (`com.example.anchornotes.data.db`)
   - Kotlin DAO interface for search operations
   - Uses @RawQuery for dynamic query building

6. **TagDao.kt** (`com.example.anchornotes.data.db`)
   - Kotlin DAO interface for tag operations
   - Provides getAllTags() for filter dialog

7. **AppDatabase.java** (Modified)
   - Updated to include TagEntity and NoteTagCrossRef in entities array
   - Added abstract methods: noteSearchDao(), tagDao()
   - Database version increased to 2
   - Added fallbackToDestructiveMigration() (TODO: implement proper migration)

### Repository Layer
8. **NoteRepositoryExtension.kt** (`com.example.anchornotes.data.repo`)
   - Kotlin class: NoteSearchRepository
   - Builds dynamic SQL queries based on NoteSearchFilter
   - Implements ANY tag matching (OR logic) - note matches if it has ANY selected tag
   - **Note**: Location filter is currently disabled (commented out) because NoteEntity doesn't have latitude/longitude fields yet

9. **ServiceLocatorExtension.kt** (`com.example.anchornotes.data`)
   - Provides noteSearchRepository() method

### ViewModel Layer
10. **NoteViewModel.kt** (`com.example.anchornotes.viewmodel`)
    - Kotlin ViewModel for search functionality
    - Uses Transformations.switchMap to react to filter changes
    - Methods:
      - `updateSearchQuery(query: String)` - updates search text
      - `updateFilters(filter: NoteSearchFilter)` - updates filters (preserves query)
      - `clearFilters()` - clears all filters
    - Exposes: searchResults (LiveData), allTags (LiveData)

### UI Layer
11. **HomeFragmentKotlin.kt** (`com.example.anchornotes.ui`)
    - Kotlin version of HomeFragment with search integration
    - Sets up SearchView to listen to text changes
    - Adds Filter button in menu
    - Observes searchResults from ViewModel
    - Listens for filter results from FilterDialogFragment via Fragment Result API

12. **FilterDialogFragment.kt** (`com.example.anchornotes.ui`)
    - Dialog fragment for filtering notes
    - Features:
      - Multi-select tag checkboxes
      - Date range pickers (from/to)
      - Toggle checkboxes: has photo, has voice, has location
      - Apply/Clear/Cancel buttons
    - Uses Fragment Result API to send filter back to parent

### Resources
13. **dialog_filter.xml** (`res/layout/`)
    - Layout for filter dialog
    - Includes: tags list, date pickers, flag checkboxes, action buttons

14. **home_menu.xml** (`res/menu/`)
    - Menu with Filter action button

15. **strings.xml** (Modified)
    - Added strings for filter UI: filter, tags, date_range, has_photo, has_voice, has_location, apply, clear, cancel

### Build Configuration
16. **app/build.gradle.kts** (Modified)
    - Added Kotlin plugins: `kotlin("android")`, `kotlin("plugin.parcelize")`
    - Added Kotlin options: jvmTarget = "17"
    - Added dependencies:
      - `lifecycle-viewmodel-ktx`
      - `lifecycle-livedata-ktx`
      - `room-ktx`

## Implementation Details

### Search Functionality
- **Search Query**: Searches in both `title` and `bodyHtml` fields using LIKE operator
- **Tag Filtering**: Uses ANY tag matching (OR logic) - a note matches if it has ANY of the selected tags
- **Date Range**: Filters by `updatedAt` field with BETWEEN operator
- **Flags**: Filters by `hasPhoto`, `hasVoice` boolean fields
- **Location**: Currently disabled (NoteEntity doesn't have location fields yet)

### Query Building
The search query is built dynamically in `NoteSearchRepository.buildSearchQuery()`:
- Only includes conditions for non-null filter values
- Uses parameterized queries for safety
- Combines all filters with AND logic

### Architecture Flow
1. **UI** (HomeFragment) → User types in SearchView or clicks Filter
2. **ViewModel** (NoteViewModel) → Receives search query or filter updates
3. **Repository** (NoteSearchRepository) → Builds dynamic SQL query
4. **DAO** (NoteSearchDao) → Executes raw query
5. **Database** (Room) → Returns filtered results
6. **ViewModel** → Transforms results to LiveData
7. **UI** → Observes LiveData and updates RecyclerView

## Usage Instructions

### To Use the New Search Functionality:

1. **Replace HomeFragment with HomeFragmentKotlin**:
   - In MainActivity or wherever you instantiate HomeFragment, replace with:
   ```kotlin
   HomeFragmentKotlin.newInstance()
   ```

2. **Ensure ViewModel is scoped to Activity**:
   - HomeFragmentKotlin uses `requireActivity()` for ViewModel scope
   - This allows sharing ViewModel with other fragments if needed

3. **Add Location Fields to NoteEntity** (when ready):
   - Add `latitude: Double?` and `longitude: Double?` fields to NoteEntity
   - Uncomment location filter code in NoteRepositoryExtension.kt
   - Update database version and add migration

4. **Database Migration**:
   - Currently using `fallbackToDestructiveMigration()` which will wipe data
   - For production, implement proper migration from version 1 to 2

## Known Issues / TODOs

1. **Location Fields**: NoteEntity doesn't have latitude/longitude fields yet. Location filter is disabled.

2. **Database Migration**: Need to implement proper migration from version 1 to 2 instead of destructive migration.

3. **NoteEntity Fields**: The existing NoteEntity uses `bodyHtml` instead of `body` - this is handled correctly in the search query.

4. **Java/Kotlin Interop**: The codebase is mixed Java/Kotlin. The new search functionality is in Kotlin, while existing code is Java. This works fine but consider migrating fully to Kotlin.

5. **Tag Management**: Currently no UI for creating/editing tags. Tags must be added programmatically or through database.

## Testing

To test the search functionality:

1. **Search by Text**:
   - Type in the SearchView
   - Results should filter in real-time

2. **Filter by Tags**:
   - Click Filter button
   - Select tags (if any exist)
   - Click Apply
   - Results should show only notes with selected tags

3. **Filter by Date**:
   - Click Filter button
   - Select from/to dates
   - Click Apply
   - Results should show notes updated in that range

4. **Filter by Flags**:
   - Click Filter button
   - Check "Has Photo", "Has Voice", or "Has Location"
   - Click Apply
   - Results should show only notes matching the selected flags

5. **Combine Filters**:
   - Use multiple filters together
   - All filters combine with AND logic

## Notes

- The implementation uses **ANY tag matching** (OR logic) - a note matches if it has ANY of the selected tags, not ALL tags.
- Search is case-insensitive (SQLite LIKE operator).
- The filter dialog preserves the current search query when applying filters.
- Results are sorted by: pinned first, then by updatedAt descending.

