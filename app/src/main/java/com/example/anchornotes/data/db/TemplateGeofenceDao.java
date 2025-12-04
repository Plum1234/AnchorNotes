package com.example.anchornotes.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for template geofences.
 * Provides methods to manage geofence locations associated with templates.
 */
@Dao
public interface TemplateGeofenceDao {

    /**
     * Insert or replace a template geofence.
     * @param geofence The geofence to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TemplateGeofenceEntity geofence);

    /**
     * Get a specific template geofence by ID.
     * @param geofenceId The geofence ID (e.g., "template-office")
     * @return The geofence entity, or null if not found
     */
    @Query("SELECT * FROM template_geofences WHERE geofenceId = :geofenceId")
    TemplateGeofenceEntity getById(String geofenceId);

    /**
     * Delete a template geofence by ID.
     * @param geofenceId The geofence ID to delete
     */
    @Query("DELETE FROM template_geofences WHERE geofenceId = :geofenceId")
    void delete(String geofenceId);

    /**
     * Get all template geofences.
     * @return List of all template geofences
     */
    @Query("SELECT * FROM template_geofences")
    List<TemplateGeofenceEntity> getAll();

    /**
     * Delete all template geofences.
     */
    @Query("DELETE FROM template_geofences")
    void deleteAll();
}
