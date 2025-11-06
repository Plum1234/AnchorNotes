package com.example.anchornotes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.TemplateEntity;
import com.example.anchornotes.data.repo.TemplateRepository;
import com.example.anchornotes.util.SingleLiveEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemplateViewModel extends AndroidViewModel {
    private final TemplateRepository repository;
    private final ExecutorService executor;

    private final MutableLiveData<List<TemplateEntity>> allTemplates = new MutableLiveData<>();
    private final MutableLiveData<List<TemplateEntity>> templatesForSelection = new MutableLiveData<>();
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> successMessage = new SingleLiveEvent<>();

    public TemplateViewModel(@NonNull Application application) {
        super(application);
        this.repository = ServiceLocator.templateRepository(application);
        this.executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> repository.ensureExampleTemplates());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    public LiveData<List<TemplateEntity>> getAllTemplates() {
        return allTemplates;
    }

    public LiveData<List<TemplateEntity>> getTemplatesForSelection() {
        return templatesForSelection;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void loadAllTemplates() {
        executor.execute(() -> {
            try {
                List<TemplateEntity> templates = repository.getAll();
                allTemplates.postValue(templates);
            } catch (Exception e) {
                errorMessage.postValue("Failed to load templates: " + e.getMessage());
            }
        });
    }

    public void loadTemplatesForSelection() {
        executor.execute(() -> {
            try {
                List<TemplateEntity> templates = repository.getTemplatesForSelection();
                templatesForSelection.postValue(templates);
            } catch (Exception e) {
                errorMessage.postValue("Failed to load templates: " + e.getMessage());
                try {
                    List<TemplateEntity> fallback = repository.getAll();
                    templatesForSelection.postValue(fallback);
                } catch (Exception ignored) {}
            }
        });
    }

    public void searchTemplates(String query) {
        executor.execute(() -> {
            try {
                List<TemplateEntity> templates = repository.search(query);
                allTemplates.postValue(templates);
            } catch (Exception e) {
                errorMessage.postValue("Search failed: " + e.getMessage());
            }
        });
    }

    public void createTemplate(String name, String pageColor, String prefilledHtml,
                              List<Long> associatedTagIds, String associatedGeofenceId) {
        executor.execute(() -> {
            try {
                if (!repository.isNameUnique(name, 0)) {
                    errorMessage.postValue("Template name already exists");
                    return;
                }

                TemplateEntity template = new TemplateEntity(
                        name,
                        pageColor,
                        prefilledHtml,
                        repository.serializeTagIds(associatedTagIds),
                        associatedGeofenceId,
                        false
                );

                long id = repository.create(template);
                if (id > 0) {
                    successMessage.postValue("Template created successfully");
                    loadAllTemplates();
                } else {
                    errorMessage.postValue("Failed to create template");
                }
            } catch (Exception e) {
                errorMessage.postValue("Failed to create template: " + e.getMessage());
            }
        });
    }

    public void updateTemplate(long templateId, String name, String pageColor,
                              String prefilledHtml, List<Long> associatedTagIds,
                              String associatedGeofenceId) {
        executor.execute(() -> {
            try {
                if (!repository.isNameUnique(name, templateId)) {
                    errorMessage.postValue("Template name already exists");
                    return;
                }

                TemplateEntity template = repository.getById(templateId);
                if (template == null) {
                    errorMessage.postValue("Template not found");
                    return;
                }

                template.name = name;
                template.pageColor = pageColor;
                template.prefilledHtml = prefilledHtml;
                template.associatedTagIds = repository.serializeTagIds(associatedTagIds);
                template.associatedGeofenceId = associatedGeofenceId;

                repository.update(template);
                successMessage.postValue("Template updated successfully");
                loadAllTemplates();
            } catch (Exception e) {
                errorMessage.postValue("Failed to update template: " + e.getMessage());
            }
        });
    }

    public void deleteTemplate(TemplateEntity template) {
        executor.execute(() -> {
            try {
                if (template.isExample) {
                    errorMessage.postValue("Cannot delete example template");
                    return;
                }

                repository.delete(template);
                successMessage.postValue("Template deleted successfully");
                loadAllTemplates();
            } catch (Exception e) {
                errorMessage.postValue("Failed to delete template: " + e.getMessage());
            }
        });
    }

    public void duplicateTemplate(TemplateEntity original) {
        executor.execute(() -> {
            try {
                String newName = original.name + " (Copy)";
                int counter = 2;
                while (!repository.isNameUnique(newName, 0)) {
                    newName = original.name + " (Copy " + counter + ")";
                    counter++;
                }

                TemplateEntity duplicate = new TemplateEntity(
                        newName,
                        original.pageColor,
                        original.prefilledHtml,
                        original.associatedTagIds,
                        original.associatedGeofenceId,
                        false
                );

                long id = repository.create(duplicate);
                if (id > 0) {
                    successMessage.postValue("Template duplicated successfully");
                    loadAllTemplates();
                } else {
                    errorMessage.postValue("Failed to duplicate template");
                }
            } catch (Exception e) {
                errorMessage.postValue("Failed to duplicate template: " + e.getMessage());
            }
        });
    }

    public TemplateEntity getTemplateById(long templateId) {
        return repository.getById(templateId);
    }

    public List<Long> parseAssociatedTagIds(String tagIdsJson) {
        return repository.parseAssociatedTagIds(tagIdsJson);
    }
}