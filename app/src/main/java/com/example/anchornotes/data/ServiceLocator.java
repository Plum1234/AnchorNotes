package com.example.anchornotes.data;

import android.content.Context;
import com.example.anchornotes.data.db.AppDatabase;
import com.example.anchornotes.data.repo.NoteRepository;

public class ServiceLocator {
    public static NoteRepository noteRepository(Context c) {
        return new NoteRepository(AppDatabase.get(c).noteDao());
    }
}
