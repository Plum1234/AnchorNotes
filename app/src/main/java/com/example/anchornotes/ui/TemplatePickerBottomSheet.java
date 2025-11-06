package com.example.anchornotes.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes.R;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.TagEntity;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.databinding.BottomSheetTemplatePickerBinding;
import com.example.anchornotes.databinding.ItemTemplatePreviewBinding;
import com.example.anchornotes.viewmodel.TemplateViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TemplatePickerBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "template_picker_result";
    public static final String TEMPLATE_ID_EXTRA = "template_id";

    private BottomSheetTemplatePickerBinding binding;
    private TemplateViewModel viewModel;
    private TemplatePickerAdapter adapter;

    public static TemplatePickerBottomSheet newInstance() {
        return new TemplatePickerBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetTemplatePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TemplateViewModel.class);

        setupRecyclerView();
        setupListeners();
        observeViewModel();
        viewModel.loadTemplatesForSelection();
    }

    private void setupRecyclerView() {
        adapter = new TemplatePickerAdapter(this::onTemplateSelected);
        binding.rvTemplates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTemplates.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBlankNote.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putLong(TEMPLATE_ID_EXTRA, -1);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });
    }

    private void observeViewModel() {
        viewModel.getTemplatesForSelection().observe(getViewLifecycleOwner(), templates -> {
            if (templates != null) {
                adapter.submitList(templates);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onTemplateSelected(TemplateEntity template) {
        Bundle result = new Bundle();
        result.putLong(TEMPLATE_ID_EXTRA, template.id);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class TemplatePickerAdapter extends RecyclerView.Adapter<TemplatePickerAdapter.ViewHolder> {
        private List<TemplateEntity> templates = new ArrayList<>();
        private final TemplateSelectedListener listener;

        interface TemplateSelectedListener {
            void onTemplateSelected(TemplateEntity template);
        }

        public TemplatePickerAdapter(TemplateSelectedListener listener) {
            this.listener = listener;
        }

        public void submitList(List<TemplateEntity> newTemplates) {
            this.templates = newTemplates != null ? newTemplates : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemTemplatePreviewBinding binding = ItemTemplatePreviewBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TemplateEntity template = templates.get(position);
            holder.bind(template);
        }

        @Override
        public int getItemCount() {
            return templates.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemTemplatePreviewBinding binding;

            ViewHolder(ItemTemplatePreviewBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.getRoot().setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onTemplateSelected(templates.get(position));
                    }
                });
            }

            void bind(TemplateEntity template) {
                binding.tvTemplateName.setText(template.name);
                if (!TextUtils.isEmpty(template.pageColor)) {
                    try {
                        int color = Color.parseColor(template.pageColor);
                        binding.colorIndicator.setBackgroundColor(color);
                    } catch (IllegalArgumentException e) {
                        binding.colorIndicator.setBackgroundColor(Color.parseColor("#E0E0E0"));
                    }
                } else {
                    binding.colorIndicator.setBackgroundColor(Color.parseColor("#E0E0E0"));
                }

                String preview = "";
                if (!TextUtils.isEmpty(template.prefilledHtml)) {
                    preview = Html.fromHtml(template.prefilledHtml, Html.FROM_HTML_MODE_COMPACT).toString().trim();
                    if (preview.length() > 100) {
                        preview = preview.substring(0, 97) + "...";
                    }
                }
                binding.tvTemplatePreview.setText(preview);

                // Show priority indicator for geofence-associated templates that are currently active
                // For simplicity we'll show it for any template with a geofence association
                // A more sophisticated implementation would check if we're currently in that geofence
                if (!TextUtils.isEmpty(template.associatedGeofenceId)) {
                    binding.tvPriorityIndicator.setVisibility(View.VISIBLE);
                } else {
                    binding.tvPriorityIndicator.setVisibility(View.GONE);
                }

                binding.chipGroupTags.removeAllViews();
                if (!TextUtils.isEmpty(template.associatedTagIds)) {
                    try {
                        org.json.JSONArray tagIds = new org.json.JSONArray(template.associatedTagIds);
                        for (int i = 0; i < Math.min(tagIds.length(), 3); i++) { // Show max 3 tags
                            Chip chip = new Chip(binding.getRoot().getContext());
                            chip.setText("Tag " + tagIds.getLong(i));
                            chip.setTextSize(10);
                            chip.setChipMinHeight(24);
                            binding.chipGroupTags.addView(chip);
                        }
                    } catch (Exception ignored) {
                        // JSON parsing failed so skip tags
                    }
                }
            }
        }
    }
}