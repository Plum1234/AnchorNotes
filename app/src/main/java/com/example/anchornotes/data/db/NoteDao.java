package com.example.anchornotes.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    List<NoteEntity> getAll();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    NoteEntity getById(long id);   // ← added

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :noteId")
    void setPinned(long noteId, boolean pinned, long updatedAt);

    @Query("UPDATE notes SET latitude = :lat, longitude = :lon, locationLabel = :label, updatedAt = :updatedAt WHERE id = :noteId")
    void updateLocation(long noteId, Double lat, Double lon, String label, long updatedAt);

}
