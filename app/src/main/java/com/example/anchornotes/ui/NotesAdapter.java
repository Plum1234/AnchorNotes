package com.example.anchornotes.ui;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes.R;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.db.NoteTagCrossRef;
import com.example.anchornotes.data.db.TagDao;
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.data.repo.NoteRepository;
import com.example.anchornotes.databinding.ItemNoteBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Adapter with section headers: “Pinned” and “Others”. */
public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnClick { void onNote(NoteEntity n); }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NOTE   = 1;

    private final OnClick onClick;

    /** Flattened list that includes header rows and notes. */
    private final List<Row> rows = new ArrayList<>();

    public NotesAdapter(OnClick onClick) { this.onClick = onClick; }

    /** Public API – submit raw notes; adapter will section them. */
    public void submit(List<NoteEntity> list) {
        buildRows(list == null ? new ArrayList<>() : new ArrayList<>(list));
        notifyDataSetChanged();
    }

    private void buildRows(List<NoteEntity> items) {
        rows.clear();
        // Sort: pinned first, then updatedAt desc
        Collections.sort(items, new Comparator<NoteEntity>() {
            @Override public int compare(NoteEntity a, NoteEntity b) {
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                return Long.compare(b.updatedAt, a.updatedAt);
            }
        });

        List<NoteEntity> pinned = new ArrayList<>();
        List<NoteEntity> others = new ArrayList<>();
        for (NoteEntity n : items) {
            if (n.pinned) pinned.add(n); else others.add(n);
        }

        if (!pinned.isEmpty()) {
            rows.add(Row.header("Pinned"));
            for (NoteEntity n : pinned) rows.add(Row.note(n));
        }
        // Always show “Others” header so the “section” is obvious,
        // but skip it if there are no others AND there was no pinned header (single section).
        if (!others.isEmpty() || pinned.isEmpty()) {
            rows.add(Row.header(pinned.isEmpty() ? "Notes" : "Others"));
            for (NoteEntity n : others) rows.add(Row.note(n));
        }
    }

    @Override public int getItemViewType(int position) { return rows.get(position).type; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            android.widget.TextView tv = new android.widget.TextView(parent.getContext());
            int pad = (int) (16 * parent.getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad / 2);

            // ↓ Replace the missing style with manual styling
            tv.setAllCaps(true);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setTextColor(0xFF666666);

            return new HeaderVH(tv);
        }
        ItemNoteBinding b = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteVH(b);
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.type == TYPE_HEADER) {
            ((HeaderVH) holder).bind(row.headerTitle);
        } else {
            ((NoteVH) holder).bind(row.note);
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    // --- Rows ---

    private static class Row {
        final int type;
        final String headerTitle;
        final NoteEntity note;

        private Row(int type, String headerTitle, NoteEntity note) {
            this.type = type; this.headerTitle = headerTitle; this.note = note;
        }
        static Row header(String title) { return new Row(TYPE_HEADER, title, null); }
        static Row note(NoteEntity n) { return new Row(TYPE_NOTE, null, n); }
    }

    // --- VHs ---

    static class HeaderVH extends RecyclerView.ViewHolder {
        private final android.widget.TextView tv;
        HeaderVH(android.widget.TextView tv) { super(tv); this.tv = tv; }
        void bind(String title) { tv.setText(title); }
    }

    class NoteVH extends RecyclerView.ViewHolder {
        ItemNoteBinding b;
        NoteVH(ItemNoteBinding b) { super(b.getRoot()); this.b = b; }

        void bind(NoteEntity n) {
            b.tvTitle.setText(n.title == null || n.title.isEmpty() ? "(Untitled)" : n.title);
            b.tvPreview.setText(Html.fromHtml(n.bodyHtml == null ? "" : n.bodyHtml, Html.FROM_HTML_MODE_LEGACY));
            // Show a star icon if pinned
            b.tvTitle.setCompoundDrawablePadding((int) (6 * b.tvTitle.getResources().getDisplayMetrics().density));
            b.tvTitle.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, n.pinned ? android.R.drawable.btn_star_big_on : 0, 0
            );

            // Tap -> open editor
            b.getRoot().setOnClickListener(v -> onClick.onNote(n));

            // Long-press -> popup menu: Pin/Unpin, Add Tag, Remove Tags
            b.getRoot().setOnLongClickListener(v -> {
                showPopup(v, n);
                return true;
            });
        }

        private void showPopup(View anchor, NoteEntity note) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            MenuInflater inflater = menu.getMenuInflater();
            menu.getMenu().add(0, 1, 0, note.pinned ? "Unpin" : "Pin");
            menu.getMenu().add(0, 2, 1, "Add Tag");
            menu.getMenu().add(0, 3, 2, "Remove Tags…");

            menu.setOnMenuItemClickListener(item -> {
                Context ctx = anchor.getContext();
                switch (item.getItemId()) {
                    case 1: { // Pin / Unpin
                        try {
                            NoteRepository repo = ServiceLocator.noteRepository(ctx);
                            boolean newPinned = !note.pinned;
                            repo.setPinned(note.id, newPinned);
                            note.pinned = newPinned;
                            note.updatedAt = System.currentTimeMillis();
                            // re-run a local resort and full refresh
                            List<NoteEntity> raw = collectNotesOnly();
                            submit(raw);
                            Toast.makeText(ctx, newPinned ? "Pinned" : "Unpinned", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(ctx, "Pin toggle failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    case 2: { // Add Tag
                        showAddTagDialog(anchor.getContext(), note);
                        return true;
                    }
                    case 3: { // Remove Tags…
                        showRemoveTagsDialog(anchor.getContext(), note);
                        return true;
                    }
                }
                return false;
            });
            menu.show();
        }

        private List<NoteEntity> collectNotesOnly() {
            List<NoteEntity> all = new ArrayList<>();
            for (Row r : rows) if (r.type == TYPE_NOTE && r.note != null) all.add(r.note);
            return all;
        }

        private void showAddTagDialog(Context ctx, NoteEntity note) {
            final EditText input = new EditText(ctx);
            input.setHint("e.g., Biology");
            new androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("Add Tag")
                    .setView(input)
                    .setPositiveButton("Add", (d, w) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) return;
                        try {
                            TagDao tagDao = ServiceLocator.tagDao(ctx);
                            TagEntity existing = tagDao.getByName(name);
                            long tagId = (existing != null) ? existing.id : tagDao.insert(new TagEntity(name));
                            ServiceLocator.refDao(ctx).insert(new NoteTagCrossRef(note.id, tagId));
                            Toast.makeText(ctx, "Tag added", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(ctx, "Add tag failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showRemoveTagsDialog(Context ctx, NoteEntity note) {
            // Simple approach: fetch all tags and let user type which to remove.
            // For a quick UX, ask for tag name to remove:
            final EditText input = new EditText(ctx);
            input.setHint("Tag name to remove");
            new androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("Remove Tag")
                    .setView(input)
                    .setPositiveButton("Remove", (d, w) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) return;
                        try {
                            TagDao tagDao = ServiceLocator.tagDao(ctx);
                            TagEntity t = tagDao.getByName(name);
                            if (t == null) {
                                Toast.makeText(ctx, "No such tag", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            ServiceLocator.refDao(ctx).delete(note.id, t.id);
                            Toast.makeText(ctx, "Tag removed", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(ctx, "Remove failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
