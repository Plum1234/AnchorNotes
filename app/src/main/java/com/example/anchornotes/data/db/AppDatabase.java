package com.example.anchornotes.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {NoteEntity.class, TagEntity.class, NoteTagCrossRef.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NoteDao noteDao();
    
    // Kotlin DAOs for search functionality
    public abstract com.example.anchornotes.data.db.NoteSearchDao noteSearchDao();
    public abstract com.example.anchornotes.data.db.TagDao tagDao();
    public abstract com.example.anchornotes.data.db.NoteTagCrossRefDao noteTagCrossRefDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "anchornotes.db"
                            )
                            // Allow main-thread DB queries (safe for early testing only)
                            .allowMainThreadQueries()
                            // TODO: Add migration when upgrading from version 1 to 2
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
