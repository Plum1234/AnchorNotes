package com.example.anchornotes.data.repo;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.db.NoteSearchDao;

import java.util.ArrayList;
import java.util.List;

public class NoteSearchRepository {
    private final NoteSearchDao dao;

    public NoteSearchRepository(NoteSearchDao dao) { this.dao = dao; }

    public List<NoteEntity> search(String queryText,
                                   List<Long> tagIds,
                                   Long fromDate,
                                   Long toDate,
                                   Boolean hasPhoto,
                                   Boolean hasVoice,
                                   Boolean hasLocation) {

        List<String> where = new ArrayList<>();
        List<Object> args  = new ArrayList<>();

        if (queryText != null && !queryText.trim().isEmpty()) {
            String pat = "%" + queryText.trim() + "%";
            where.add("(title LIKE ? OR bodyHtml LIKE ?)");
            args.add(pat); args.add(pat);
        }

        if (fromDate != null && toDate != null) { where.add("updatedAt BETWEEN ? AND ?"); args.add(fromDate); args.add(toDate); }
        else if (fromDate != null) { where.add("updatedAt >= ?"); args.add(fromDate); }
        else if (toDate != null) { where.add("updatedAt <= ?"); args.add(toDate); }

        if (Boolean.TRUE.equals(hasPhoto)) where.add("hasPhoto = 1");
        if (Boolean.TRUE.equals(hasVoice)) where.add("hasVoice = 1");

        // location filter (enable if NoteEntity has latitude/longitude)
        if (Boolean.TRUE.equals(hasLocation)) {
            where.add("latitude IS NOT NULL AND longitude IS NOT NULL");
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            StringBuilder ph = new StringBuilder();
            for (int i = 0; i < tagIds.size(); i++) {
                if (i > 0) ph.append(',');
                ph.append('?');
            }
            where.add("id IN (SELECT DISTINCT noteId FROM note_tag_cross_ref WHERE tagId IN (" + ph + "))");
            args.addAll(tagIds);
        }

        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        String sql = "SELECT DISTINCT * FROM notes" + whereSql + " ORDER BY pinned DESC, updatedAt DESC";

        SupportSQLiteQuery q = new SimpleSQLiteQuery(sql, args.toArray());
        return dao.search(q);
    }
}
