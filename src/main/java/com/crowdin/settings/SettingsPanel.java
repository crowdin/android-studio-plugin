package com.crowdin.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.util.Optional;

public class SettingsPanel {

    private final JPanel mainPanel;

    private final JBTextField projectId = new JBTextField();
    private final JBPasswordField apiToken = new JBPasswordField();
    private final JBTextField baseUrl = new JBTextField();
    private final JBTextField completionFileExtensions = new JBTextField();

    private final JBCheckBox doNotShowConfirmation = new JBCheckBox("Do not show confirmation dialogs ");
    private final JBCheckBox useGitBranch = new JBCheckBox("Use Git Branch ");
    private final JBCheckBox autoUpload = new JBCheckBox("Automatically upload on change ");
    private final JBCheckBox enableCompletion = new JBCheckBox("Enable Autocompletion ");

    public SettingsPanel() {
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Login"))
                .addSeparator()
                .addLabeledComponent(new JBLabel("Project id *: "), projectId, 2)
                .addLabeledComponent(new JBLabel("Api token *: "), apiToken, 2)
                .addLabeledComponent(new JBLabel("Base URL: "), baseUrl, 2)
                .addComponent(new JBLabel("Settings"), 20)
                .addSeparator()
                .addComponent(useGitBranch, 5)
                .addComponent(autoUpload, 5)
                .addComponent(doNotShowConfirmation, 5)
                .addComponent(new JBLabel("Autocomplete"), 10)
                .addSeparator()
                .addComponent(enableCompletion, 5)
                .addLabeledComponent(new JBLabel("File extensions"), completionFileExtensions, 5)
                .addComponent(new JBLabel("Comma-separated list of file extensions for which autocomplete should be active."))
                .addComponent(new JBLabel("By default strings autocomplete will be active in all files."))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void setProjectId(String projectId) {
        this.projectId.setText(Optional.ofNullable(projectId).orElse(""));
    }

    public String getProjectId() {
        return this.projectId.getText();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl.setText(baseUrl);
    }

    public String getBaseUrl() {
        return this.baseUrl.getText();
    }

    public void setApiToken(String apiToken) {
        this.apiToken.setText(apiToken);
    }

    public String getApiToken() {
        return this.apiToken.getText();
    }

    public void setCompletionFileExtensions(String text) {
        this.completionFileExtensions.setText(text);
    }

    public String getCompletionFileExtensions() {
        return this.completionFileExtensions.getText();
    }

    public void setDoNotShowConfirmation(boolean doNotShowConfirmation) {
        this.doNotShowConfirmation.setSelected(doNotShowConfirmation);
    }

    public boolean getDoNotShowConfirmation() {
        return this.doNotShowConfirmation.isSelected();
    }

    public void setUseGitBranch(boolean useGitBranch) {
        this.useGitBranch.setSelected(useGitBranch);
    }

    public boolean getUseGitBranch() {
        return this.useGitBranch.isSelected();
    }

    public void setAutoUpload(boolean autoUpload) {
        this.autoUpload.setSelected(autoUpload);
    }

    public boolean getAutoUpload() {
        return this.autoUpload.isSelected();
    }

    public void setEnableCompletion(boolean enableCompletion) {
        this.enableCompletion.setSelected(enableCompletion);
    }

    public boolean getEnableCompletion() {
        return this.enableCompletion.isSelected();
    }
}
