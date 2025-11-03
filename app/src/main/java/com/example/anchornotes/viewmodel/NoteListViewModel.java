package com.example.anchornotes.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.NoteEntity;
import java.util.List;

public class NoteListViewModel extends AndroidViewModel {
    public NoteListViewModel(@NonNull Application app) {
        super(app);
    }

    public List<NoteEntity> loadNotes() {
        return ServiceLocator.noteRepository(getApplication()).getAll();
    }
}
