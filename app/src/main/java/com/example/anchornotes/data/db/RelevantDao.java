package com.example.anchornotes.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RelevantDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(RelevantNoteEntity entity);
    
    @Query("DELETE FROM relevant WHERE noteId = :noteId")
    void delete(long noteId);
    
    @Query("DELETE FROM relevant WHERE expiresAt <= :now")
    int expire(long now);
    
    @Query("SELECT * FROM relevant WHERE expiresAt > :now ORDER BY expiresAt ASC")
    LiveData<List<RelevantNoteEntity>> liveRelevant(long now);
    
    @Query("SELECT * FROM relevant WHERE noteId = :noteId LIMIT 1")
    RelevantNoteEntity getByNoteId(long noteId);
}
