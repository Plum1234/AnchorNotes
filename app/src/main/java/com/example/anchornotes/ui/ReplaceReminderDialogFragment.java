package com.example.anchornotes.ui;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.anchornotes.R;
import com.example.anchornotes.model.PlaceSelection;
import com.example.anchornotes.model.ReminderType;
import com.example.anchornotes.viewmodel.NoteViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Dialog fragment for confirming replacement of existing reminder.
 */
public class ReplaceReminderDialogFragment extends DialogFragment {
    private static final String ARG_NOTE_ID = "noteId";
    private static final String ARG_NEW_TYPE = "newType";
    private static final String ARG_AT_MILLIS = "atMillis";

    private NoteViewModel viewModel;
    private PlaceSelection place;

    public static ReplaceReminderDialogFragment newInstance(long noteId, ReminderType newType, long atMillis, PlaceSelection place) {
        ReplaceReminderDialogFragment fragment = new ReplaceReminderDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        args.putString(ARG_NEW_TYPE, newType.name());
        args.putLong(ARG_AT_MILLIS, atMillis);
        // PlaceSelection is not Parcelable, so we'll pass coordinates separately if needed
        if (place != null) {
            args.putDouble("lat", place.latitude);
            args.putDouble("lon", place.longitude);
            args.putFloat("radius", place.radiusMeters);
            args.putString("label", place.label);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        long noteId = args.getLong(ARG_NOTE_ID);
        ReminderType newType = ReminderType.valueOf(args.getString(ARG_NEW_TYPE));
        long atMillis = args.getLong(ARG_AT_MILLIS);

        if (newType == ReminderType.GEOFENCE && args.containsKey("lat")) {
            place = new PlaceSelection(
                    args.getDouble("lat"),
                    args.getDouble("lon"),
                    args.getFloat("radius"),
                    args.getString("label", "Location")
            );
        }

        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(NoteViewModel.class);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Replace Reminder?");
        builder.setMessage("This note already has a reminder. Replace it?");
        builder.setPositiveButton("Replace", (dialog, which) -> {
            viewModel.onConfirmReplace(newType, noteId, atMillis, place);
            dismiss();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dismiss());

        return builder.create();
    }
}
