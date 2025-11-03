package com.example.anchornotes.data.repo;

import com.example.anchornotes.data.db.NoteDao;
import com.example.anchornotes.data.db.NoteEntity;

import java.util.List;

public class NoteRepository {

    private final NoteDao dao;

    public NoteRepository(NoteDao dao) {
        this.dao = dao;
    }

    public List<NoteEntity> getAll() {
        return dao.getAll();
    }

    /** Get one note for editor prefill. */
    public NoteEntity get(long id) {                // ← added
        return dao.getById(id);
    }

    /** Create or update a note; preserves createdAt on update. */
    public long createOrUpdate(Long id,
                               String title,
                               String bodyHtml,
                               String photoUri,
                               String voiceUri,
                               boolean pinned) {
        long now = System.currentTimeMillis();

        if (id == null || id == 0L) {
            NoteEntity e = new NoteEntity(
                    0,
                    safe(title),
                    safe(bodyHtml),
                    notEmpty(photoUri),
                    photoUri,
                    notEmpty(voiceUri),
                    voiceUri,
                    pinned,
                    now,         // createdAt
                    now          // updatedAt
            );
            return dao.insert(e);
        } else {
            NoteEntity old = dao.getById(id);
            long created = (old != null ? old.createdAt : now); // preserve
            NoteEntity e = new NoteEntity(
                    id,
                    safe(title),
                    safe(bodyHtml),
                    notEmpty(photoUri),
                    photoUri,
                    notEmpty(voiceUri),
                    voiceUri,
                    pinned,
                    created,     // keep original createdAt
                    now          // updatedAt
            );
            dao.update(e);
            return id;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static boolean notEmpty(String s) { return s != null && !s.isEmpty(); }
}
