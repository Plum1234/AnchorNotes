package com.example.anchornotes.context;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.anchornotes.receiver.GeofenceReceiver;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages geofence registration and removal using Play Services Location API.
 */
public class GeofenceManager {
    private final Context context;
    private final GeofencingClient geofencingClient;

    public GeofenceManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    /**
     * Adds a geofence for a note.
     * @param noteId The note ID
     * @param lat Latitude of the geofence center
     * @param lon Longitude of the geofence center
     * @param radiusMeters Radius in meters (typically 150-200m)
     * @param geofenceId Unique geofence ID (typically "note-{noteId}")
     * @param onDone Called when geofence is successfully added (with true)
     * @param onError Called if geofence addition fails
     */
    public void addForNote(long noteId, double lat, double lon, float radiusMeters,
                           String geofenceId,
                           @NonNull Consumer<Boolean> onDone,
                           @NonNull Consumer<Exception> onError) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(lat, lon, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(0)
                .build();

        List<Geofence> geofences = new ArrayList<>();
        geofences.add(geofence);

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();

        Intent intent = new Intent(context, GeofenceReceiver.class);
        intent.putExtra(GeofenceReceiver.EXTRA_NOTE_ID, noteId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener(v -> onDone.accept(true))
                .addOnFailureListener(onError::accept);
    }

    /**
     * Removes a geofence for a note.
     * @param geofenceId The geofence ID to remove
     */
    public void removeForNote(String geofenceId) {
        List<String> ids = new ArrayList<>();
        ids.add(geofenceId);
        geofencingClient.removeGeofences(ids);
    }
}
