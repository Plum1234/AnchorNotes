package com.example.anchornotes.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.anchornotes.R;
import com.example.anchornotes.databinding.DialogLocationPickerBinding;
import com.example.anchornotes.model.PlaceSelection;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Dialog fragment for selecting a location using OpenStreetMap or current location.
 * Allows users to choose custom locations for geofence reminders.
 * No API key required - uses OSMDroid.
 */
public class LocationPickerDialogFragment extends DialogFragment {

    private DialogLocationPickerBinding binding;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    private GeoPoint selectedGeoPoint;
    private Marker selectedMarker;
    private float selectedRadius = 175.0f; // Default radius in meters
    private String selectedLabel = "Selected Location";

    private LocationSelectedListener listener;

    /**
     * Interface for listening to location selection events.
     */
    public interface LocationSelectedListener {
        void onLocationSelected(PlaceSelection placeSelection);
    }

    public static LocationPickerDialogFragment newInstance() {
        return new LocationPickerDialogFragment();
    }

    public void setLocationSelectedListener(LocationSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogLocationPickerBinding.inflate(LayoutInflater.from(requireContext()));

        // Initialize MapView
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Set default location (USC Campus)
        GeoPoint startPoint = new GeoPoint(34.0224, -118.2851);
        mapView.getController().setCenter(startPoint);

        setupMapClickListener();
        setupTabs();
        setupRadiusSelector();
        setupButtons();

        // Try to move to current location if permission granted
        moveToCurrentLocationIfAvailable();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private void setupMapClickListener() {
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                onMapClick(geoPoint);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint geoPoint) {
                return false;
            }
        };

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void onMapClick(GeoPoint geoPoint) {
        // Remove old marker if exists
        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
        }

        // Add new marker
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(geoPoint);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle("Selected Location");
        mapView.getOverlays().add(selectedMarker);
        mapView.invalidate();

        selectedGeoPoint = geoPoint;

        // Try to get address from coordinates
        updateLocationLabel(geoPoint);

        // Enable confirm button
        binding.btnConfirm.setEnabled(true);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Map tab
                    binding.mapView.setVisibility(View.VISIBLE);
                    binding.llCurrentLocation.setVisibility(View.GONE);
                } else if (position == 1) {
                    // Current Location tab
                    binding.mapView.setVisibility(View.GONE);
                    binding.llCurrentLocation.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set up current location button
        binding.btnGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());
    }

    private void setupRadiusSelector() {
        // SeekBar range: 50m to 500m (seekbar max=450, progress=125 means 175m)
        binding.radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedRadius = 50 + progress; // 50m minimum + progress
                binding.tvRadiusValue.setText(String.format(Locale.US, "%.0fm", selectedRadius));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnConfirm.setOnClickListener(v -> {
            if (selectedGeoPoint != null && listener != null) {
                PlaceSelection selection = new PlaceSelection(
                        selectedGeoPoint.getLatitude(),
                        selectedGeoPoint.getLongitude(),
                        selectedRadius,
                        selectedLabel
                );
                listener.onLocationSelected(selection);
                dismiss();
            } else {
                Toast.makeText(requireContext(), "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveToCurrentLocationIfAvailable() {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().setCenter(currentGeoPoint);
                    }
                });
            } catch (SecurityException e) {
                // Permission denied
            }
        }
    }

    private void getCurrentLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvCurrentLocationInfo.setText("Getting location...");

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (location != null) {
                            selectedGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            updateLocationLabel(selectedGeoPoint);
                            binding.tvCurrentLocationInfo.setText(
                                    String.format(Locale.US, "Location: %.6f, %.6f\n%s",
                                            location.getLatitude(), location.getLongitude(), selectedLabel)
                            );
                            binding.btnConfirm.setEnabled(true);
                        } else {
                            binding.tvCurrentLocationInfo.setText("Unable to get location. Try again.");
                            Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvCurrentLocationInfo.setText("Failed to get location");
                        Toast.makeText(requireContext(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationLabel(GeoPoint geoPoint) {
        // Try to get address using Geocoder
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    geoPoint.getLatitude(),
                    geoPoint.getLongitude(),
                    1
            );
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Try to get a nice label
                if (address.getFeatureName() != null) {
                    selectedLabel = address.getFeatureName();
                } else if (address.getThoroughfare() != null) {
                    selectedLabel = address.getThoroughfare();
                } else if (address.getLocality() != null) {
                    selectedLabel = address.getLocality();
                } else {
                    selectedLabel = String.format(Locale.US, "%.4f, %.4f",
                            geoPoint.getLatitude(), geoPoint.getLongitude());
                }
            } else {
                selectedLabel = String.format(Locale.US, "%.4f, %.4f",
                        geoPoint.getLatitude(), geoPoint.getLongitude());
            }
        } catch (IOException e) {
            selectedLabel = String.format(Locale.US, "%.4f, %.4f",
                    geoPoint.getLatitude(), geoPoint.getLongitude());
        }

        binding.tvSelectedLocationInfo.setText("Selected: " + selectedLabel);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
