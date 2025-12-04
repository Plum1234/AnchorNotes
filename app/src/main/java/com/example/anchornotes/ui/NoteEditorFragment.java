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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.data.repo.TemplateRepository;
import com.example.anchornotes.databinding.FragmentNoteEditorBinding;
import com.example.anchornotes.model.ReminderType;
import com.example.anchornotes.viewmodel.NoteEditorViewModel;
import com.example.anchornotes.viewmodel.NoteViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteEditorFragment extends Fragment {
    private static final String ARG_ID = "id";
    private static final String ARG_TEMPLATE_ID = "template_id";
    private FragmentNoteEditorBinding b;
    private Long noteId;
    private Long templateId;
    private String photoUri;
    private String voicePath;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private NoteEditorViewModel vm;

    // --- Location state ---
    private FusedLocationProviderClient fused;
    private Double noteLat, noteLon;
    private String noteLocLabel;
    
    // --- Pin state ---
    private boolean isPinned = false;

    public static NoteEditorFragment newInstance(@Nullable Long id) {
        return newInstance(id, null);
    }

    public static NoteEditorFragment newInstance(@Nullable Long id, @Nullable Long templateId) {
        NoteEditorFragment f = new NoteEditorFragment();
        Bundle args = new Bundle();
        if (id != null) args.putLong(ARG_ID, id);
        if (templateId != null && templateId > 0) args.putLong(ARG_TEMPLATE_ID, templateId);
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
        setHasOptionsMenu(true); // Enable menu
        if (getArguments()!=null && getArguments().containsKey(ARG_ID))
            noteId = getArguments().getLong(ARG_ID);
        if (getArguments()!=null && getArguments().containsKey(ARG_TEMPLATE_ID))
            templateId = getArguments().getLong(ARG_TEMPLATE_ID);
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
        // Set up toolbar with back button
        androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) requireActivity();
        androidx.appcompat.widget.Toolbar toolbar = activity.findViewById(com.example.anchornotes.R.id.toolbar);
        if (toolbar != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
        
        setupFormatting();

        /* ---------- Prefill when editing or from template ---------- */
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
                    // --- prefill pinned state ---
                    isPinned = n.pinned;
                    // --- load and display tags ---
                    loadAndDisplayTags();
                }
            } catch (Exception ignored) {}
        } else if (templateId != null) {
            // Apply template for new note creation
            applyTemplate(templateId);
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

        // --- Add tag button/chip ---
        addAddTagChip();

        // Hide Save and Reminder buttons from layout (they're now in toolbar)
        b.btnSave.setVisibility(View.GONE);
        b.btnReminder.setVisibility(View.GONE);
    }

    /* ===================== Toolbar Menu ===================== */

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(com.example.anchornotes.R.menu.note_editor_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Update pin icon based on current state
        MenuItem pinItem = menu.findItem(com.example.anchornotes.R.id.action_pin);
        if (pinItem != null) {
            updatePinIcon(pinItem);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            // Back button
            requireActivity().getSupportFragmentManager().popBackStack();
            return true;
        } else if (id == com.example.anchornotes.R.id.action_pin) {
            // Toggle pin
            togglePin(item);
            return true;
        } else if (id == com.example.anchornotes.R.id.action_save) {
            // Save note
            saveNote();
            return true;
        } else if (id == com.example.anchornotes.R.id.action_reminder) {
            // Set reminder
            showReminderDialog();
            return true;
        } else if (id == com.example.anchornotes.R.id.action_share) {
            // Share note
            shareNote();
            return true;
        } else if (id == com.example.anchornotes.R.id.action_duplicate) {
            // Duplicate note
            duplicateNote();
            return true;
        } else if (id == com.example.anchornotes.R.id.action_delete) {
            // Delete note
            deleteNote();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void updatePinIcon(MenuItem pinItem) {
        if (isPinned) {
            pinItem.setIcon(android.R.drawable.btn_star_big_on);
            pinItem.setTitle("Unpin");
        } else {
            pinItem.setIcon(android.R.drawable.btn_star_big_off);
            pinItem.setTitle("Pin");
        }
    }

    private void togglePin(MenuItem item) {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isPinned = !isPinned;
        ServiceLocator.noteRepository(requireContext()).setPinned(noteId, isPinned);
        updatePinIcon(item);
        Toast.makeText(requireContext(), isPinned ? "Pinned" : "Unpinned", Toast.LENGTH_SHORT).show();
    }

    private void saveNote() {
        String title = b.etTitle.getText().toString().trim();
        String bodyHtml = Html.toHtml(b.etBody.getText());

        boolean isNew = (noteId == null);
        long savedId = vm.save(noteId, title, bodyHtml, photoUri, voicePath, isPinned);
        noteId = savedId;

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
        if (!isNew) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void showReminderDialog() {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        ReminderDialogFragment dialog = ReminderDialogFragment.newInstance(noteId);
        dialog.show(getParentFragmentManager(), "ReminderDialog");
    }

    private void shareNote() {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        NoteEntity note = vm.load(noteId);
        if (note == null) {
            Toast.makeText(requireContext(), "Note not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String text = (note.title != null ? note.title + "\n\n" : "") +
                (note.bodyHtml != null ? android.text.Html.fromHtml(note.bodyHtml, android.text.Html.FROM_HTML_MODE_LEGACY).toString() : "");
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share note"));
    }

    private void duplicateNote() {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        NoteEntity note = vm.load(noteId);
        if (note == null) {
            Toast.makeText(requireContext(), "Note not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a new note with the same content
        long newId = vm.save(null, note.title, note.bodyHtml, note.photoUri, note.voiceUri, false);
        Toast.makeText(requireContext(), "Note duplicated", Toast.LENGTH_SHORT).show();
        
        // Open the duplicated note
        androidx.fragment.app.FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager().beginTransaction();
        ft.replace(com.example.anchornotes.R.id.fragment_container, NoteEditorFragment.newInstance(newId));
        ft.addToBackStack(null);
        ft.commit();
    }

    private void deleteNote() {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Note?")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (d, w) -> {
                    // Delete the note using repository (handles cleanup of geofences, reminders, etc.)
                    ServiceLocator.noteRepository(requireContext()).deleteNote(noteId);
                    Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                    // Navigate back to home list
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private void applyTemplate(long templateId) {
        try {
            TemplateRepository templateRepo = ServiceLocator.templateRepository(requireContext());
            TemplateEntity template = templateRepo.getById(templateId);

            if (template == null) {
                Toast.makeText(requireContext(), "Template not found", Toast.LENGTH_SHORT).show();
                updateLocationButtonLabel();
                return;
            }

            // Apply background color
            if (template.pageColor != null && !template.pageColor.isEmpty()) {
                try {
                    int color = android.graphics.Color.parseColor(template.pageColor);
                    // Apply to the main container
                    b.getRoot().setBackgroundColor(color);
                } catch (IllegalArgumentException e) {
                    // Invalid color format, ignore
                }
            }

            // Apply prefilled content
            if (template.prefilledHtml != null && !template.prefilledHtml.isEmpty()) {
                b.etBody.setText(Html.fromHtml(template.prefilledHtml, Html.FROM_HTML_MODE_COMPACT));
            }

            // Apply associated tags as chips
            if (template.associatedTagIds != null && !template.associatedTagIds.isEmpty()) {
                java.util.List<Long> tagIds = templateRepo.parseAssociatedTagIds(template.associatedTagIds);
                if (!tagIds.isEmpty()) {
                    // Load tag names and add them as chips
                    loadAndDisplayTagsFromIds(tagIds);
                }
            }

            // Set template name as default title if no title is set
            if (template.name != null && !template.name.isEmpty()) {
                String defaultTitle = template.name.replace("Template", "").replace("template", "").trim();
                if (!defaultTitle.isEmpty()) {
                    b.etTitle.setHint("New " + defaultTitle);
                }
            }

            updateLocationButtonLabel();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error applying template: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateLocationButtonLabel();
        }
    }

    private void loadAndDisplayTagsFromIds(java.util.List<Long> tagIds) {
        try {
            for (Long tagId : tagIds) {
                TagEntity tag = ServiceLocator.tagDao(requireContext()).getById(tagId);
                if (tag != null) {
                    Toast.makeText(requireContext(), "Applied tag: " + tag.name, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            // Failed to load tags, continue anyway
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (noteId != null && b != null) {
            NoteEntity note = vm.load(noteId);
            if (note != null) {
                updateReminderButtonText(note);
                // Update pinned state
                isPinned = note.pinned;
                // Reload tags
                loadAndDisplayTags();
            }
        }
        // Invalidate menu to update pin icon
        requireActivity().invalidateOptionsMenu();
    }

    @Override public void onStop() {
        super.onStop();
        cleanupRecorder();
        try { if (player != null) player.release(); } catch (Exception ignored) {}
        player = null;
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }

    /* ===================== Tag Management ===================== */

    private void loadAndDisplayTags() {
        if (noteId == null || b == null) return;
        
        new Thread(() -> {
            try {
                List<TagEntity> tags = ServiceLocator.noteRepository(requireContext()).getTagsForNote(noteId);
                requireActivity().runOnUiThread(() -> {
                    displayTags(tags);
                });
            } catch (Exception e) {
                // Failed to load tags, continue anyway
            }
        }).start();
    }

    private void displayTags(List<TagEntity> tags) {
        if (b == null) return;
        b.chipGroupTags.removeAllViews();
        
        for (TagEntity tag : tags) {
            Chip chip = createTagChip(tag);
            b.chipGroupTags.addView(chip);
        }
        
        // Always show "Add tag" chip at the end
        addAddTagChip();
    }

    private Chip createTagChip(TagEntity tag) {
        Chip chip = new Chip(requireContext());
        chip.setText(tag.name);
        chip.setCloseIconVisible(true);
        chip.setCloseIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        chip.setOnCloseIconClickListener(v -> {
            if (noteId == null) {
                Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
                return;
            }
            ServiceLocator.noteRepository(requireContext()).removeTagFromNote(noteId, tag.id);
            // Remove chip from view immediately
            b.chipGroupTags.removeView(chip);
            // Ensure "Add tag" chip is still present
            addAddTagChip();
            Toast.makeText(requireContext(), "Tag removed", Toast.LENGTH_SHORT).show();
        });
        return chip;
    }

    private void addAddTagChip() {
        if (b == null) return;
        
        // Remove existing "Add tag" chip if present
        for (int i = 0; i < b.chipGroupTags.getChildCount(); i++) {
            View child = b.chipGroupTags.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.getText().toString().equals("+ Add tag")) {
                    b.chipGroupTags.removeView(chip);
                    break;
                }
            }
        }
        
        Chip addChip = new Chip(requireContext());
        addChip.setText("+ Add tag");
        addChip.setChipIconResource(android.R.drawable.ic_input_add);
        addChip.setOnClickListener(v -> showTagSelectionDialog());
        b.chipGroupTags.addView(addChip);
    }

    private void showTagSelectionDialog() {
        if (noteId == null) {
            Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                List<TagEntity> allTags = ServiceLocator.noteRepository(requireContext()).getAllTags();
                List<TagEntity> currentTags = ServiceLocator.noteRepository(requireContext()).getTagsForNote(noteId);
                Set<Long> currentTagIds = new HashSet<>();
                for (TagEntity tag : currentTags) {
                    currentTagIds.add(tag.id);
                }
                
                String[] tagNames = new String[allTags.size()];
                boolean[] checked = new boolean[allTags.size()];
                for (int i = 0; i < allTags.size(); i++) {
                    tagNames[i] = allTags.get(i).name;
                    checked[i] = currentTagIds.contains(allTags.get(i).id);
                }
                
                requireActivity().runOnUiThread(() -> {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Select Tags")
                            .setMultiChoiceItems(tagNames, checked, (dialog, which, isChecked) -> {
                                checked[which] = isChecked;
                            })
                            .setPositiveButton("Apply", (dialog, which) -> {
                                applyTagSelections(allTags, checked);
                            })
                            .setNegativeButton("Cancel", null)
                            .setNeutralButton("Create New", (dialog, which) -> {
                                showCreateTagDialog();
                            })
                            .show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error loading tags: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void applyTagSelections(List<TagEntity> allTags, boolean[] checked) {
        if (noteId == null) return;
        
        new Thread(() -> {
            try {
                NoteRepository repo = ServiceLocator.noteRepository(requireContext());
                List<TagEntity> currentTags = repo.getTagsForNote(noteId);
                Set<Long> currentTagIds = new HashSet<>();
                for (TagEntity tag : currentTags) {
                    currentTagIds.add(tag.id);
                }
                
                // Remove unchecked tags
                for (int i = 0; i < allTags.size(); i++) {
                    if (!checked[i] && currentTagIds.contains(allTags.get(i).id)) {
                        repo.removeTagFromNote(noteId, allTags.get(i).id);
                    }
                }
                
                // Add checked tags
                for (int i = 0; i < allTags.size(); i++) {
                    if (checked[i] && !currentTagIds.contains(allTags.get(i).id)) {
                        repo.addTagToNote(noteId, allTags.get(i).name);
                    }
                }
                
                // Reload and display tags
                requireActivity().runOnUiThread(() -> {
                    loadAndDisplayTags();
                    Toast.makeText(requireContext(), "Tags updated", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error updating tags: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showCreateTagDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("e.g., Biology");
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Create New Tag")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    if (noteId == null) {
                        Toast.makeText(requireContext(), "Please save the note first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new Thread(() -> {
                        try {
                            ServiceLocator.noteRepository(requireContext()).addTagToNote(noteId, name);
                            requireActivity().runOnUiThread(() -> {
                                loadAndDisplayTags();
                                Toast.makeText(requireContext(), "Tag added", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Error adding tag: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
