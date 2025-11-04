package com.example.anchornotes.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<TagEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TagEntity tag);

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    TagEntity getByName(String name);
}
