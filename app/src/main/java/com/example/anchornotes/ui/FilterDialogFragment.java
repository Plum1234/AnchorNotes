package com.example.anchornotes.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.anchornotes.R;
import com.example.anchornotes.data.NoteSearchFilter;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.TagDao;
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.databinding.DialogFilterBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FilterDialogFragment extends DialogFragment {
    public static final String TAG = "FilterDialogFragment";
    public static final String RESULT_KEY = "filter_result";
    public static final String FILTER_EXTRA = "filter";

    private DialogFilterBinding b;
    private final java.util.Set<Long> selectedTagIds = new java.util.HashSet<>();
    private Long fromDate = null, toDate = null;

    public static FilterDialogFragment newInstance() { return new FilterDialogFragment(); }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.setTitle(R.string.filter_notes);
        return d;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = DialogFilterBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load tags (synchronously, allowed because DB allows main thread queries)
        TagDao tagDao = ServiceLocator.tagDao(requireContext());
        List<TagEntity> tags = tagDao.getAll();
        populateTags(tags);

        b.btnFromDate.setOnClickListener(v -> showDatePicker(true));
        b.btnToDate.setOnClickListener(v -> showDatePicker(false));

        b.btnApply.setOnClickListener(v -> {
            NoteSearchFilter f = new NoteSearchFilter();
            f.tagIds.addAll(selectedTagIds);
            f.fromDate = fromDate;
            f.toDate = toDate;
            f.hasPhoto = b.checkboxHasPhoto.isChecked() ? Boolean.TRUE : null;
            f.hasVoice = b.checkboxHasVoice.isChecked() ? Boolean.TRUE : null;
            f.hasLocation = b.checkboxHasLocation.isChecked() ? Boolean.TRUE : null;

            Bundle result = new Bundle();
            result.putParcelable(FILTER_EXTRA, f);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });

        b.btnClear.setOnClickListener(v -> {
            selectedTagIds.clear();
            fromDate = toDate = null;
            b.btnFromDate.setText(R.string.select_from_date);
            b.btnToDate.setText(R.string.select_to_date);
            b.checkboxHasPhoto.setChecked(false);
            b.checkboxHasVoice.setChecked(false);
            b.checkboxHasLocation.setChecked(false);
            clearTagChecks();
        });

        b.btnCancel.setOnClickListener(v -> dismiss());
    }

    private void populateTags(List<TagEntity> tags) {
        b.layoutTags.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText(getString(R.string.no_tags_available));
            tv.setPadding(16, 16, 16, 16);
            b.layoutTags.addView(tv);
            return;
        }
        for (TagEntity t : tags) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(t.name);
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedTagIds.add(t.id);
                else selectedTagIds.remove(t.id);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            b.layoutTags.addView(cb, lp);
        }
    }

    private void clearTagChecks() {
        for (int i = 0; i < b.layoutTags.getChildCount(); i++) {
            View v = b.layoutTags.getChildAt(i);
            if (v instanceof CheckBox) ((CheckBox) v).setChecked(false);
        }
    }

    private void showDatePicker(boolean isFrom) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth, 0, 0, 0);
            long ts = sel.getTimeInMillis();
            SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            if (isFrom) { fromDate = ts; b.btnFromDate.setText(fmt.format(sel.getTime())); }
            else { toDate = ts; b.btnToDate.setText(fmt.format(sel.getTime())); }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
