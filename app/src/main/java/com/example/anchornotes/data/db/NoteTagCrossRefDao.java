package com.example.anchornotes.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface NoteTagCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NoteTagCrossRef ref);

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    void delete(long noteId, long tagId);
}
