package com.example.anchornotes.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.anchornotes.R;
import com.example.anchornotes.databinding.DialogCreateTemplateBinding;
import com.example.anchornotes.viewmodel.TemplateViewModel;

public class CreateTemplateDialog extends DialogFragment {
    private DialogCreateTemplateBinding binding;
    private TemplateViewModel viewModel;
    private String selectedColor = "#E3F2FD"; // Default blue

    public static CreateTemplateDialog newInstance() {
        return new CreateTemplateDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(TemplateViewModel.class);

        binding = DialogCreateTemplateBinding.inflate(LayoutInflater.from(requireContext()));

        setupColorPickers();
        setupButtons();

        return new AlertDialog.Builder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private void setupColorPickers() {
        binding.colorBlue.setBackgroundColor(Color.parseColor("#1976D2")); // Darker to show selection

        binding.colorBlue.setOnClickListener(v -> selectColor("#E3F2FD", binding.colorBlue));
        binding.colorGreen.setOnClickListener(v -> selectColor("#E8F5E8", binding.colorGreen));
        binding.colorYellow.setOnClickListener(v -> selectColor("#FFF9C4", binding.colorYellow));
        binding.colorPink.setOnClickListener(v -> selectColor("#FCE4EC", binding.colorPink));
        binding.colorWhite.setOnClickListener(v -> selectColor("#FFFFFF", binding.colorWhite));
    }

    private void selectColor(String color, View selectedView) {
        selectedColor = color;

        // Reset all colors to their original state
        binding.colorBlue.setBackgroundColor(Color.parseColor("#E3F2FD"));
        binding.colorGreen.setBackgroundColor(Color.parseColor("#E8F5E8"));
        binding.colorYellow.setBackgroundColor(Color.parseColor("#FFF9C4"));
        binding.colorPink.setBackgroundColor(Color.parseColor("#FCE4EC"));
        binding.colorWhite.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // Darken the selected color to show selection
        if (color.equals("#E3F2FD")) {
            selectedView.setBackgroundColor(Color.parseColor("#1976D2"));
        } else if (color.equals("#E8F5E8")) {
            selectedView.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if (color.equals("#FFF9C4")) {
            selectedView.setBackgroundColor(Color.parseColor("#FBC02D"));
        } else if (color.equals("#FCE4EC")) {
            selectedView.setBackgroundColor(Color.parseColor("#E91E63"));
        } else if (color.equals("#FFFFFF")) {
            selectedView.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnCreate.setOnClickListener(v -> {
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

            // Create the template
            viewModel.createTemplate(name, selectedColor, content, null, null);
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}