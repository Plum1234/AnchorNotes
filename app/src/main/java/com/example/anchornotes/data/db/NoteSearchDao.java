package com.example.anchornotes.data.db;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.List;

@Dao
public interface NoteSearchDao {
    @RawQuery(observedEntities = {NoteEntity.class})
    List<NoteEntity> search(SupportSQLiteQuery query);
}
