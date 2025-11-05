package com.example.anchornotes.data.db;

import android.content.Context;

import androidx.annotation.NonNull;                       // ← add
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;                // ← add
import androidx.sqlite.db.SupportSQLiteDatabase;        // ← add

@Database(
        entities = {
                NoteEntity.class,
                TagEntity.class,
                NoteTagCrossRef.class,
                RelevantNoteEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NoteDao noteDao();
    public abstract TagDao tagDao();
    public abstract NoteTagCrossRefDao noteTagCrossRefDao();
    public abstract NoteSearchDao noteSearchDao();
    public abstract RelevantDao relevantDao();

    private static volatile AppDatabase INSTANCE;

    // v1 -> v2 migration: add (optional) location columns and create tag & cross-ref tables
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // these try/catch blocks make the migration idempotent if columns already exist
            try { db.execSQL("ALTER TABLE notes ADD COLUMN latitude REAL"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE notes ADD COLUMN longitude REAL"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE notes ADD COLUMN locationLabel TEXT"); } catch (Exception ignored) {}

            // tags table (+ unique index on name)
            db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)");

            // cross-ref table — make sure this NAME matches your @Entity(tableName)
            db.execSQL("CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (" +
                    "`noteId` INTEGER NOT NULL, " +
                    "`tagId` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`noteId`,`tagId`), " +
                    "FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`)");
        }
    };

    // v2 -> v3 migration: add reminder fields and relevant table
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add reminder columns to notes table
            try { db.execSQL("ALTER TABLE notes ADD COLUMN reminderType TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE notes ADD COLUMN geofenceId TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE notes ADD COLUMN pendingActivation INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}

            // Create relevant table
            db.execSQL("CREATE TABLE IF NOT EXISTS `relevant` (" +
                    "`noteId` INTEGER PRIMARY KEY NOT NULL, " +
                    "`expiresAt` INTEGER NOT NULL)");
        }
    };

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "anchornotes.db"
                            )
                            .allowMainThreadQueries()          // OK for class project / quick testing
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)      // preserves data across versions
                            // .fallbackToDestructiveMigration() // dev-only alternative if you want a wipe
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
