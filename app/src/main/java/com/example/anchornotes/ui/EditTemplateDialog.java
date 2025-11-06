package com.example.anchornotes.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.anchornotes.R;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.databinding.DialogEditTemplateBinding;
import com.example.anchornotes.viewmodel.TemplateViewModel;

public class EditTemplateDialog extends DialogFragment {
    private static final String ARG_TEMPLATE = "template";

    private DialogEditTemplateBinding binding;
    private TemplateViewModel viewModel;
    private String selectedColor = "#E3F2FD"; // Default blue
    private TemplateEntity templateToEdit;

    public static EditTemplateDialog newInstance(TemplateEntity template) {
        EditTemplateDialog dialog = new EditTemplateDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TEMPLATE, template);
        dialog.setArguments(args);
        return dialog;
    }

    public static EditTemplateDialog newInstance() {
        return new EditTemplateDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(TemplateViewModel.class);
        binding = DialogEditTemplateBinding.inflate(LayoutInflater.from(requireContext()));

        // Check if we're editing an existing template
        if (getArguments() != null && getArguments().containsKey(ARG_TEMPLATE)) {
            templateToEdit = (TemplateEntity) getArguments().getSerializable(ARG_TEMPLATE);
            binding.tvTitle.setText("Edit Template");
            loadTemplateData();
        } else {
            binding.tvTitle.setText("Create Template");
        }

        setupColorPickers();
        setupFormattingButtons();
        setupButtons();

        return new AlertDialog.Builder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private void loadTemplateData() {
        if (templateToEdit == null) return;

        binding.etTemplateName.setText(templateToEdit.name);

        // Set selected color
        if (!TextUtils.isEmpty(templateToEdit.pageColor)) {
            selectedColor = templateToEdit.pageColor;
            updateColorSelection();
        }

        // Set content
        if (!TextUtils.isEmpty(templateToEdit.prefilledHtml)) {
            binding.etTemplateContent.setText(Html.fromHtml(templateToEdit.prefilledHtml, Html.FROM_HTML_MODE_COMPACT));
        }
    }

    private void setupColorPickers() {
        updateColorSelection(); // Set default selection

        binding.colorBlue.setOnClickListener(v -> selectColor("#E3F2FD"));
        binding.colorGreen.setOnClickListener(v -> selectColor("#E8F5E8"));
        binding.colorYellow.setOnClickListener(v -> selectColor("#FFF9C4"));
        binding.colorPink.setOnClickListener(v -> selectColor("#FCE4EC"));
        binding.colorWhite.setOnClickListener(v -> selectColor("#FFFFFF"));
    }

    private void selectColor(String color) {
        selectedColor = color;
        updateColorSelection();
    }

    private void updateColorSelection() {
        // Reset all colors to their original state
        binding.colorBlue.setBackgroundColor(Color.parseColor("#E3F2FD"));
        binding.colorGreen.setBackgroundColor(Color.parseColor("#E8F5E8"));
        binding.colorYellow.setBackgroundColor(Color.parseColor("#FFF9C4"));
        binding.colorPink.setBackgroundColor(Color.parseColor("#FCE4EC"));
        binding.colorWhite.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // Darken the selected color to show selection
        View selectedView = null;
        int darkerColor = 0;

        switch (selectedColor) {
            case "#E3F2FD":
                selectedView = binding.colorBlue;
                darkerColor = Color.parseColor("#1976D2");
                break;
            case "#E8F5E8":
                selectedView = binding.colorGreen;
                darkerColor = Color.parseColor("#4CAF50");
                break;
            case "#FFF9C4":
                selectedView = binding.colorYellow;
                darkerColor = Color.parseColor("#FBC02D");
                break;
            case "#FCE4EC":
                selectedView = binding.colorPink;
                darkerColor = Color.parseColor("#E91E63");
                break;
            case "#FFFFFF":
                selectedView = binding.colorWhite;
                darkerColor = Color.parseColor("#9E9E9E");
                break;
        }

        if (selectedView != null) {
            selectedView.setBackgroundColor(darkerColor);
        }
    }

    private void setupFormattingButtons() {
        binding.btnBold.setOnClickListener(v -> applyFormatting(new StyleSpan(Typeface.BOLD)));
        binding.btnItalic.setOnClickListener(v -> applyFormatting(new StyleSpan(Typeface.ITALIC)));
        binding.btnH1.setOnClickListener(v -> applyFormatting(new RelativeSizeSpan(1.5f)));
        binding.btnH2.setOnClickListener(v -> applyFormatting(new RelativeSizeSpan(1.3f)));
        binding.btnList.setOnClickListener(v -> insertChecklistItem());
    }

    private void applyFormatting(Object span) {
        int start = binding.etTemplateContent.getSelectionStart();
        int end = binding.etTemplateContent.getSelectionEnd();

        if (start == end) {
            Toast.makeText(requireContext(), "Please select text to format", Toast.LENGTH_SHORT).show();
            return;
        }

        Editable editable = binding.etTemplateContent.getText();
        if (editable instanceof SpannableStringBuilder) {
            SpannableStringBuilder spannable = (SpannableStringBuilder) editable;
            spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void insertChecklistItem() {
        int cursorPosition = binding.etTemplateContent.getSelectionStart();
        String checklistItem = "☐ ";

        Editable editable = binding.etTemplateContent.getText();
        if (editable != null) {
            editable.insert(cursorPosition, checklistItem);
        }
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.etTemplateName.getText().toString().trim();
            String content = binding.etTemplateContent.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.etTemplateName.setError("Template name is required");
                return;
            }

            if (TextUtils.isEmpty(content)) {
                binding.etTemplateContent.setError("Template content is required");
                return;
            }

            // Convert rich text to HTML
            String htmlContent = Html.toHtml(binding.etTemplateContent.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

            if (templateToEdit != null) {
                // Update existing template
                viewModel.updateTemplate(templateToEdit.id, name, selectedColor, htmlContent, null, null);
            } else {
                // Create new template
                viewModel.createTemplate(name, selectedColor, htmlContent, null, null);
            }
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}