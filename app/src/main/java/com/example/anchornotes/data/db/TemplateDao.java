package com.example.anchornotes.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TemplateDao {

    @Insert
    long insert(TemplateEntity template);

    @Update
    void update(TemplateEntity template);

    @Delete
    void delete(TemplateEntity template);

    @Query("SELECT * FROM templates WHERE id = :id")
    TemplateEntity getById(long id);

    @Query("SELECT * FROM templates ORDER BY updatedAt DESC")
    List<TemplateEntity> getAll();

    @Query("SELECT * FROM templates WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    List<TemplateEntity> search(String query);

    // Geofence-prioritized ordering - templates with matching geofenceId first
    @Query("SELECT * FROM templates ORDER BY " +
           "CASE WHEN associatedGeofenceId IN (:currentGeofenceIds) THEN 0 ELSE 1 END, " +
           "updatedAt DESC")
    List<TemplateEntity> getTemplatesOrderedByGeofence(List<String> currentGeofenceIds);

    // Get templates with no geofence association first, then by date
    @Query("SELECT * FROM templates ORDER BY " +
           "CASE WHEN associatedGeofenceId IS NULL THEN 0 ELSE 1 END, " +
           "updatedAt DESC")
    List<TemplateEntity> getTemplatesNonGeofenceFirst();

    @Query("SELECT * FROM templates WHERE isExample = 1")
    List<TemplateEntity> getExampleTemplates();

    @Query("SELECT COUNT(*) FROM templates WHERE isExample = 1")
    int getExampleTemplateCount();

    @Query("SELECT COUNT(*) FROM templates WHERE name = :name AND id != :excludeId")
    int countByNameExcluding(String name, long excludeId);

    @Query("SELECT COUNT(*) FROM templates WHERE name = :name")
    int countByName(String name);
}