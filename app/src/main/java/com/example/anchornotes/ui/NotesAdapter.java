package com.example.anchornotes.ui;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.databinding.ItemNoteBinding;
import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.VH> {
    public interface OnClick { void onNote(NoteEntity n); }
    private final OnClick onClick;
    private final List<NoteEntity> items = new ArrayList<>();

    public NotesAdapter(OnClick onClick) { this.onClick = onClick; }

    public void submit(List<NoteEntity> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemNoteBinding b = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override public void onBindViewHolder(VH h, int pos) { h.bind(items.get(pos)); }

    @Override public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        ItemNoteBinding b;
        VH(ItemNoteBinding b) { super(b.getRoot()); this.b = b; }

        void bind(NoteEntity n) {
            b.tvTitle.setText(n.title == null || n.title.isEmpty() ? "(Untitled)" : n.title);
            b.tvPreview.setText(Html.fromHtml(
                    n.bodyHtml == null ? "" : n.bodyHtml,
                    Html.FROM_HTML_MODE_LEGACY));
            b.getRoot().setOnClickListener(v -> onClick.onNote(n));
        }
    }
}
