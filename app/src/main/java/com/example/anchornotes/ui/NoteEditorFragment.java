package com.example.anchornotes.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.anchornotes.databinding.FragmentNoteEditorBinding;
import com.example.anchornotes.viewmodel.NoteEditorViewModel;

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

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments()!=null && getArguments().containsKey(ARG_ID))
            noteId = getArguments().getLong(ARG_ID);
        vm = new ViewModelProvider(this).get(NoteEditorViewModel.class);
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
                com.example.anchornotes.data.db.NoteEntity n = vm.load(noteId);
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
                }
            } catch (Exception ignored) {}
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

        b.btnSave.setOnClickListener(v -> {
            String title = b.etTitle.getText().toString().trim();
            String bodyHtml = Html.toHtml(b.etBody.getText());
            vm.save(noteId, title, bodyHtml, photoUri, voicePath, false);
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
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

    @Override public void onStop() {
        super.onStop();
        cleanupRecorder();
        try { if (player != null) player.release(); } catch (Exception ignored) {}
        player = null;
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
