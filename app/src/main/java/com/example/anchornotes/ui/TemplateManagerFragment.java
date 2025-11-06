package com.example.anchornotes.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes.R;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.databinding.FragmentTemplateManagerBinding;
import com.example.anchornotes.databinding.ItemTemplateManagerBinding;
import com.example.anchornotes.viewmodel.TemplateViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TemplateManagerFragment extends Fragment {
    private FragmentTemplateManagerBinding binding;
    private TemplateViewModel viewModel;
    private TemplateManagerAdapter adapter;

    public static TemplateManagerFragment newInstance() {
        return new TemplateManagerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TemplateViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTemplateManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupFab();
        observeViewModel();

        viewModel.loadAllTemplates();
    }

    private void setupRecyclerView() {
        adapter = new TemplateManagerAdapter(this::onTemplateAction);
        binding.rvTemplates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTemplates.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabNewTemplate.setOnClickListener(v -> {
            EditTemplateDialog dialog = EditTemplateDialog.newInstance();
            dialog.show(getChildFragmentManager(), "CreateTemplate");
        });
    }

    private void observeViewModel() {
        viewModel.getAllTemplates().observe(getViewLifecycleOwner(), templates -> {
            if (templates != null) {
                adapter.submitList(templates);
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onTemplateAction(TemplateEntity template, TemplateAction action) {
        switch (action) {
            case EDIT:
                EditTemplateDialog editDialog = EditTemplateDialog.newInstance(template);
                editDialog.show(getChildFragmentManager(), "EditTemplate");
                break;
            case DUPLICATE:
                viewModel.duplicateTemplate(template);
                break;
            case DELETE:
                if (template.isExample) {
                    Toast.makeText(requireContext(), "Cannot delete example template", Toast.LENGTH_SHORT).show();
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete Template?")
                            .setMessage("Are you sure you want to delete \"" + template.name + "\"?")
                            .setPositiveButton("Delete", (d, w) -> viewModel.deleteTemplate(template))
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    enum TemplateAction {
        EDIT, DUPLICATE, DELETE
    }

    interface TemplateActionListener {
        void onTemplateAction(TemplateEntity template, TemplateAction action);
    }

    private static class TemplateManagerAdapter extends RecyclerView.Adapter<TemplateManagerAdapter.ViewHolder> {
        private List<TemplateEntity> templates = new ArrayList<>();
        private final TemplateActionListener listener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        public TemplateManagerAdapter(TemplateActionListener listener) {
            this.listener = listener;
        }

        public void submitList(List<TemplateEntity> newTemplates) {
            this.templates = newTemplates != null ? newTemplates : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemTemplateManagerBinding binding = ItemTemplateManagerBinding.inflate(
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
            private final ItemTemplateManagerBinding binding;

            ViewHolder(ItemTemplateManagerBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
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
                    if (preview.length() > 150) {
                        preview = preview.substring(0, 147) + "...";
                    }
                }
                binding.tvTemplatePreview.setText(preview);

                String info = "Created " + dateFormat.format(new Date(template.createdAt));
                if (template.isExample) {
                    info += " • Example";
                }
                if (!TextUtils.isEmpty(template.associatedGeofenceId)) {
                    info += " • Location-based";
                }
                binding.tvTemplateInfo.setText(info);

                binding.btnMore.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(v.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Edit");
                    popup.getMenu().add(0, 2, 1, "Duplicate");
                    if (!template.isExample) {
                        popup.getMenu().add(0, 3, 2, "Delete");
                    }

                    popup.setOnMenuItemClickListener(item -> {
                        if (listener != null) {
                            if (item.getItemId() == 1) {
                                listener.onTemplateAction(template, TemplateAction.EDIT);
                            } else if (item.getItemId() == 2) {
                                listener.onTemplateAction(template, TemplateAction.DUPLICATE);
                            } else if (item.getItemId() == 3) {
                                listener.onTemplateAction(template, TemplateAction.DELETE);
                            }
                        }
                        return true;
                    });
                    popup.show();
                });
            }
        }
    }
}