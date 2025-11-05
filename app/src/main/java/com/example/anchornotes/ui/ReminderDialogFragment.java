package com.example.anchornotes.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.anchornotes.R;
import com.example.anchornotes.model.PlaceSelection;
import com.example.anchornotes.viewmodel.NoteViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

/**
 * Dialog fragment for setting reminders on notes.
 */
public class ReminderDialogFragment extends DialogFragment {
    private static final String ARG_NOTE_ID = "noteId";

    private long noteId;
    private RadioGroup rgReminderType;
    private LinearLayout llTimeOptions;
    private LinearLayout llGeofenceOptions;
    private Button btnSelectDateTime;
    private Button btnSelectPlace;
    private TextView tvPlaceInfo;
    private long selectedDateTime = -1;
    private PlaceSelection selectedPlace;
    private NoteViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    public static ReminderDialogFragment newInstance(long noteId) {
        ReminderDialogFragment fragment = new ReminderDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            noteId = getArguments().getLong(ARG_NOTE_ID);
        }
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_reminder, null);

        rgReminderType = view.findViewById(R.id.rgReminderType);
        llTimeOptions = view.findViewById(R.id.llTimeOptions);
        llGeofenceOptions = view.findViewById(R.id.llGeofenceOptions);
        btnSelectDateTime = view.findViewById(R.id.btnSelectDateTime);
        btnSelectPlace = view.findViewById(R.id.btnSelectPlace);
        tvPlaceInfo = view.findViewById(R.id.tvPlaceInfo);

        rgReminderType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTime) {
                llTimeOptions.setVisibility(View.VISIBLE);
                llGeofenceOptions.setVisibility(View.GONE);
            } else {
                llTimeOptions.setVisibility(View.GONE);
                llGeofenceOptions.setVisibility(View.VISIBLE);
            }
        });

        btnSelectDateTime.setOnClickListener(v -> showDateTimePicker());
        btnSelectPlace.setOnClickListener(v -> getCurrentLocation());

        Button btnSet = view.findViewById(R.id.btnSet);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnSet.setOnClickListener(v -> {
            if (rgReminderType.getCheckedRadioButtonId() == R.id.rbTime) {
                if (selectedDateTime == -1) {
                    Toast.makeText(requireContext(), "Please select a date and time", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.onSetTimeReminder(noteId, selectedDateTime);
            } else {
                if (selectedPlace == null) {
                    Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.onSetGeofenceReminder(noteId, selectedPlace);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            calendar.set(y, m, d);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            new TimePickerDialog(requireContext(), (timeView, h, min) -> {
                calendar.set(Calendar.HOUR_OF_DAY, h);
                calendar.set(Calendar.MINUTE, min);
                selectedDateTime = calendar.getTimeInMillis();
                btnSelectDateTime.setText(android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", calendar));
            }, hour, minute, false).show();
        }, year, month, day).show();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        selectedPlace = new PlaceSelection(
                                location.getLatitude(),
                                location.getLongitude(),
                                175.0f,
                                "Current Location"
                        );
                        tvPlaceInfo.setText("Location: " + location.getLatitude() + ", " + location.getLongitude());
                    } else {
                        Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
                });
    }
}
