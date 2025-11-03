package com.example.anchornotes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.NoteEntity;

public class NoteEditorViewModel extends AndroidViewModel {

    public NoteEditorViewModel(@NonNull Application app) {
        super(app);
    }

    public long save(Long id, String title, String bodyHtml,
                     String photoUri, String voiceUri, boolean pinned) {
        return ServiceLocator.noteRepository(getApplication())
                .createOrUpdate(id, title, bodyHtml, photoUri, voiceUri, pinned);
    }

    /** Load a single note for editing. */
    public NoteEntity load(long id) {                           // ← added
        return ServiceLocator.noteRepository(getApplication()).get(id);
    }
}
