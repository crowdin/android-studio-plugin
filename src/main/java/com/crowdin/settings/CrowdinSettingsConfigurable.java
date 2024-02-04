package com.crowdin.settings;

import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class CrowdinSettingsConfigurable implements Configurable {

    private final Project project;

    private SettingsPanel settingsPanel;

    public CrowdinSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Crowdin";
    }

    @Override
    public @Nullable JComponent createComponent() {
        this.settingsPanel = new SettingsPanel();
        return this.settingsPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        CrowdingSettingsState instance = CrowdingSettingsState.getInstance(this.project);
        return !Objects.equals(this.settingsPanel.getApiToken(), instance.getApiToken()) ||
                !Objects.equals(this.settingsPanel.getBaseUrl(), instance.baseUrl) ||
                !Objects.equals(this.settingsPanel.getProjectId(), instance.projectId) ||
                !Objects.equals(this.settingsPanel.getCompletionFileExtensions(), instance.fileExtensions) ||
                !Objects.equals(this.settingsPanel.getAutoUpload(), instance.autoUpload) ||
                !Objects.equals(this.settingsPanel.getDisableBranches(), instance.disableBranches) ||
                !Objects.equals(this.settingsPanel.getDisableCompletion(), instance.disableCompletion) ||
                !Objects.equals(this.settingsPanel.getDoNotShowConfirmation(), instance.doNotShowConfirmation);
    }

    @Override
    public void apply() {
        CrowdingSettingsState instance = CrowdingSettingsState.getInstance(this.project);
        boolean needToReload = !Objects.equals(instance.projectId, this.settingsPanel.getProjectId()) ||
                !Objects.equals(instance.getApiToken(), this.settingsPanel.getApiToken());
        instance.projectId = this.settingsPanel.getProjectId();
        instance.saveApiToken(this.settingsPanel.getApiToken());
        instance.baseUrl = this.settingsPanel.getBaseUrl();
        instance.fileExtensions = this.settingsPanel.getCompletionFileExtensions();
        instance.doNotShowConfirmation = this.settingsPanel.getDoNotShowConfirmation();
        instance.autoUpload = this.settingsPanel.getAutoUpload();
        instance.disableBranches = this.settingsPanel.getDisableBranches();
        instance.disableCompletion = this.settingsPanel.getDisableCompletion();
        if (needToReload) {
            ApplicationManager.getApplication().invokeAndWait(() -> CrowdinPanelWindowFactory.reloadPanels(project, true));
        }
    }

    @Override
    public void reset() {
        CrowdingSettingsState instance = CrowdingSettingsState.getInstance(this.project);
        this.settingsPanel.setProjectId(instance.projectId);
        this.settingsPanel.setApiToken(instance.getApiToken());
        this.settingsPanel.setBaseUrl(instance.baseUrl);
        this.settingsPanel.setCompletionFileExtensions(instance.fileExtensions);
        this.settingsPanel.setDoNotShowConfirmation(instance.doNotShowConfirmation);
        this.settingsPanel.setAutoUpload(instance.autoUpload);
        this.settingsPanel.setDisableBranches(instance.disableBranches);
        this.settingsPanel.setDisableCompletion(instance.disableCompletion);
    }

    @Override
    public void disposeUIResources() {
        this.settingsPanel = null;
    }
}
