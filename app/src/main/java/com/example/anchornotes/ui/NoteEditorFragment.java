package com.example.anchornotes.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.databinding.FragmentNoteEditorBinding;
import com.example.anchornotes.model.ReminderType;
import com.example.anchornotes.viewmodel.NoteEditorViewModel;
import com.example.anchornotes.viewmodel.NoteViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;

public class NoteEditorFragment extends Fragment {
    private static final String ARG_ID = "id";
    private FragmentNoteEditorBinding b;
    private Long noteId;
    private String photoUri;
    private String voicePath;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private NoteEditorViewModel vm;

    // --- Location state ---
    private FusedLocationProviderClient fused;
    private Double noteLat, noteLon;
    private String noteLocLabel;

    public static NoteEditorFragment newInstance(@Nullable Long id) {
        NoteEditorFragment f = new NoteEditorFragment();
        Bundle args = new Bundle();
        if (id != null) args.putLong(ARG_ID, id);
        f.setArguments(args);
        return f;
    }

    /* --------------------- Pick image --------------------- */
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                photoUri = uri.toString();
                b.imgPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).into(b.imgPreview);
            });

    /* --------------------- Mic permission --------------------- */
    private final ActivityResultLauncher<String> micPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startRecording();
                else Toast.makeText(requireContext(), "Microphone permission is required", Toast.LENGTH_SHORT).show();
            });

    private boolean hasMicPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /* --------------------- Location permission --------------------- */
    private final ActivityResultLauncher<String> locPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) onAddOrUpdateLocation();
                else Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            });

    private boolean hasLocPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments()!=null && getArguments().containsKey(ARG_ID))
            noteId = getArguments().getLong(ARG_ID);
        vm = new ViewModelProvider(this).get(NoteEditorViewModel.class);
        fused = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentNoteEditorBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupFormatting();

        /* ---------- Prefill when editing ---------- */
        if (noteId != null) {
            try {
                NoteEntity n = vm.load(noteId);
                if (n != null) {
                    b.etTitle.setText(n.title == null ? "" : n.title);
                    b.etBody.setText(Html.fromHtml(
                            n.bodyHtml == null ? "" : n.bodyHtml,
                            Html.FROM_HTML_MODE_LEGACY));
                    if (n.photoUri != null && !n.photoUri.isEmpty()) {
                        photoUri = n.photoUri;
                        b.imgPreview.setVisibility(View.VISIBLE);
                        Glide.with(this).load(n.photoUri).into(b.imgPreview);
                    }
                    if (n.voiceUri != null && !n.voiceUri.isEmpty()) {
                        voicePath = n.voiceUri;
                        b.btnPlay.setEnabled(true);
                    }
                    // --- prefill location ---
                    noteLat = n.latitude;
                    noteLon = n.longitude;
                    noteLocLabel = n.locationLabel;
                    updateLocationButtonLabel();
                }
            } catch (Exception ignored) {}
        } else {
            updateLocationButtonLabel();
        }

        /* ---------- UI actions ---------- */
        b.btnAddPhoto.setOnClickListener(v -> pickImage.launch("image/*"));

        // Record is a toggle; request permission on first tap if needed
        b.btnRecord.setOnClickListener(v -> {
            if (!hasMicPermission()) {
                micPerm.launch(Manifest.permission.RECORD_AUDIO);
                return;
            }
            if (recorder == null) startRecording(); else stopRecording();
        });

        b.btnPlay.setOnClickListener(v -> playVoice());

        // --- Location button ---
        b.btnLocation.setOnClickListener(v -> showLocationActions());

        // --- Reminder button ---
        b.btnReminder.setOnClickListener(v -> {
            if (noteId == null) {
                Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
                return;
            }
            ReminderDialogFragment dialog = ReminderDialogFragment.newInstance(noteId);
            dialog.show(getParentFragmentManager(), "ReminderDialog");
        });

        // Long press on reminder button to clear reminder
        b.btnReminder.setOnLongClickListener(v -> {
            if (noteId == null) return false;
            NoteEntity note = vm.load(noteId);
            if (note != null && note.reminderType != null) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Clear Reminder?")
                        .setMessage("Remove the reminder for this note?")
                        .setPositiveButton("Clear", (d, w) -> {
                            NoteViewModel noteVm = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
                            noteVm.onClearReminder(noteId);
                            NoteEntity updated = vm.load(noteId);
                            if (updated != null) {
                                updateReminderButtonText(updated);
                            }
                            Toast.makeText(requireContext(), "Reminder cleared", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
            return false;
        });

        // Update reminder button text if note has a reminder
        if (noteId != null) {
            NoteEntity n = vm.load(noteId);
            if (n != null && n.reminderType != null) {
                updateReminderButtonText(n);
            }
        }

        b.btnSave.setOnClickListener(v -> {
            String title = b.etTitle.getText().toString().trim();
            String bodyHtml = Html.toHtml(b.etBody.getText());

            boolean isNew = (noteId == null);
            long savedId = vm.save(noteId, title, bodyHtml, photoUri, voicePath, false);
            noteId = savedId;

            // Update reminder button text after saving
            NoteEntity savedNote = vm.load(savedId);
            if (savedNote != null) {
                updateReminderButtonText(savedNote);
            }

            // Ask to update location on edit (if we have permission)
            if (!isNew && hasLocPermission()) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setMessage("Update location to current?")
                        .setPositiveButton("Yes", (d,w) -> onAddOrUpdateLocation())
                        .setNegativeButton("No", null)
                        .show();
            }

            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            // Don't pop back stack if we just created a new note - allow user to set reminder
            if (isNew) {
                // Note saved, reminder button is now enabled
            } else {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    /* ===================== Formatting ===================== */

    private void setupFormatting() {
        b.btnBold.setOnClickListener(v -> applyStyleToSelectionOrWord(android.graphics.Typeface.BOLD));
        b.btnItalic.setOnClickListener(v -> applyStyleToSelectionOrWord(android.graphics.Typeface.ITALIC));
        b.btnH1.setOnClickListener(v -> applyHeadingToCurrentLine(1));
        b.btnH2.setOnClickListener(v -> applyHeadingToCurrentLine(2));
        b.btnChecklist.setOnClickListener(v -> insertLinePrefixSafe("☐ "));
    }

    /** Bold/italic: if no selection, style the word under the caret. */
    private void applyStyleToSelectionOrWord(int style) {
        Editable text = b.etBody.getText();
        if (text == null) return;

        int start = b.etBody.getSelectionStart();
        int end   = b.etBody.getSelectionEnd();

        if (start == end) {
            int[] word = getWordBounds(text, start);
            start = word[0];
            end   = word[1];
            if (start >= end) return;
        }
        text.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /** Headings: bold + resize the entire current line (no '#' insertion). */
    private void applyHeadingToCurrentLine(int level) {
        Editable text = b.etBody.getText();
        if (text == null) return;

        int caret = Math.max(0, b.etBody.getSelectionStart());
        int[] bounds = getLineBounds(text, caret);
        int start = bounds[0], end = bounds[1];
        if (start >= end) return;

        // clear prior size spans on this line
        RelativeSizeSpan[] sizes = text.getSpans(start, end, RelativeSizeSpan.class);
        for (RelativeSizeSpan s : sizes) text.removeSpan(s);

        // apply bold + size factor
        float factor = (level == 1) ? 1.6f : 1.3f;
        text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new RelativeSizeSpan(factor),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private int[] getLineBounds(CharSequence cs, int pos) {
        int n = cs.length();
        int s = Math.max(0, Math.min(pos, n));
        int e = s;
        while (s > 0 && cs.charAt(s - 1) != '\n') s--;
        while (e < n && cs.charAt(e) != '\n') e++;
        return new int[]{s, e};
    }

    private int[] getWordBounds(CharSequence cs, int pos) {
        int n = cs.length();
        int s = Math.max(0, Math.min(pos, n));
        int e = s;
        while (s > 0 && (Character.isLetterOrDigit(cs.charAt(s - 1)) || cs.charAt(s - 1) == '_')) s--;
        while (e < n && (Character.isLetterOrDigit(cs.charAt(e)) || cs.charAt(e) == '_')) e++;
        return new int[]{s, e};
    }

    /** Insert a prefix at the start of the current line (used for checklist). */
    private void insertLinePrefixSafe(String prefix) {
        Editable text = b.etBody.getText();
        if (text == null) return;

        int caret = Math.max(0, b.etBody.getSelectionStart());
        int lineStart = caret;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') lineStart--;
        text.insert(lineStart, prefix);
    }

    /* ===================== Location helpers ===================== */

    private void updateLocationButtonLabel() {
        if (b == null) return;
        if (noteLat != null && noteLon != null) {
            b.btnLocation.setText("Location • View/Update/Remove");
        } else {
            b.btnLocation.setText("Add Location");
        }
    }

    private void updateReminderButtonText(NoteEntity note) {
        if (b == null) return;
        if (note.reminderType == null) {
            b.btnReminder.setText("Set Reminder");
        } else if ("TIME".equals(note.reminderType)) {
            if (note.reminderAt != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
                b.btnReminder.setText("Reminder: " + sdf.format(new java.util.Date(note.reminderAt)));
            } else {
                b.btnReminder.setText("Reminder: Time");
            }
        } else if ("GEOFENCE".equals(note.reminderType)) {
            String location = note.locationLabel != null ? note.locationLabel : "Location";
            b.btnReminder.setText("Reminder: " + location);
        } else {
            b.btnReminder.setText("Set Reminder");
        }
    }

    private void showLocationActions() {
        if (noteLat == null || noteLon == null) {
            // no location yet – just add
            onAddOrUpdateLocation();
            return;
        }
        String[] items = new String[]{"View on Map", "Update to Current", "Remove Location"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Location")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: viewOnMap(); break;
                        case 1: onAddOrUpdateLocation(); break;
                        case 2: removeLocation(); break;
                    }
                })
                .show();
    }

    private void viewOnMap() {
        if (noteLat == null || noteLon == null) {
            Toast.makeText(requireContext(), "No location saved", Toast.LENGTH_SHORT).show();
            return;
        }
        String label = (noteLocLabel == null || noteLocLabel.isEmpty()) ? "Note location" : noteLocLabel;
        String geo = String.format(java.util.Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                noteLat, noteLon, noteLat, noteLon, label);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geo)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No maps app installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAddOrUpdateLocation() {
        if (!hasLocPermission()) {
            locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        try {
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc == null) {
                    Toast.makeText(requireContext(), "Could not get location", Toast.LENGTH_SHORT).show();
                    return;
                }
                Double lat = loc.getLatitude();
                Double lon = loc.getLongitude();
                String label = "Current location";

                // If note not yet saved, save a draft first to get an id
                if (noteId == null) {
                    String title = b.etTitle.getText().toString().trim();
                    String bodyHtml = Html.toHtml(b.etBody.getText());
                    long id = vm.save(null, title, bodyHtml, photoUri, voicePath, false);
                    noteId = id;
                }

                ServiceLocator.noteRepository(requireContext())
                        .setLocation(noteId, lat, lon, label);

                noteLat = lat; noteLon = lon; noteLocLabel = label;
                updateLocationButtonLabel();
                Toast.makeText(requireContext(), "Location saved", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeLocation() {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        ServiceLocator.noteRepository(requireContext())
                .setLocation(noteId, null, null, null);
        noteLat = noteLon = null; noteLocLabel = null;
        updateLocationButtonLabel();
        Toast.makeText(requireContext(), "Location removed", Toast.LENGTH_SHORT).show();
    }

    /* ===================== Audio ===================== */

    private void startRecording() {
        try {
            File out = File.createTempFile("voice_", ".m4a", requireContext().getCacheDir());
            voicePath = out.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(voicePath);
            recorder.prepare();
            recorder.start();

            b.btnRecord.setText("Stop");
            b.btnPlay.setEnabled(false);
            Toast.makeText(requireContext(), "Recording…", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Record error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            cleanupRecorder();
        }
    }

    private void stopRecording() {
        try {
            if (recorder != null) recorder.stop();
        } catch (Exception ignored) {
        } finally {
            cleanupRecorder();
            b.btnRecord.setText("Record");
            b.btnPlay.setEnabled(voicePath != null);
            Toast.makeText(requireContext(), "Recording saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void cleanupRecorder() {
        try { if (recorder != null) recorder.release(); } catch (Exception ignored) {}
        recorder = null;
    }

    private void playVoice() {
        if (voicePath == null) {
            Toast.makeText(requireContext(), "No recording", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
                b.btnPlay.setText("Play");
                return;
            }
            player = new MediaPlayer();
            player.setDataSource(voicePath);
            player.prepare();
            player.start();
            b.btnPlay.setText("Stop");
            player.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) {}
                player = null;
                b.btnPlay.setText("Play");
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Play error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            try { if (player != null) player.release(); } catch (Exception ignored) {}
            player = null;
            b.btnPlay.setText("Play");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh reminder button text when returning to this fragment
        if (noteId != null && b != null) {
            NoteEntity note = vm.load(noteId);
            if (note != null) {
                updateReminderButtonText(note);
            }
        }
    }

    @Override public void onStop() {
        super.onStop();
        cleanupRecorder();
        try { if (player != null) player.release(); } catch (Exception ignored) {}
        player = null;
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
