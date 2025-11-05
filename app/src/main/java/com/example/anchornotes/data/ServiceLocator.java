package com.example.anchornotes.data;

import android.content.Context;

import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.db.NoteTagCrossRefDao;
import com.example.anchornotes.data.db.TagDao;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.data.repo.NoteSearchRepository;

public class ServiceLocator {

    public static NoteRepository noteRepository(Context c) {
        AppDatabase db = AppDatabase.get(c);
        return new NoteRepository(db.noteDao(), db.relevantDao(), c);
    }

    /** Search repository used by HomeFragment for query + filters. */
    public static NoteSearchRepository noteSearchRepository(Context c) {
        // If your NoteSearchRepository constructor needs more params,
        // add them here (e.g., tagDao, db). For most cases the search DAO is enough.
        return new NoteSearchRepository(AppDatabase.get(c).noteSearchDao());
    }

    public static TagDao tagDao(Context c) {
        return AppDatabase.get(c).tagDao();
    }

    public static NoteTagCrossRefDao refDao(Context c) {
        return AppDatabase.get(c).noteTagCrossRefDao();
    }
}
