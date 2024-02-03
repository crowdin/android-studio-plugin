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

    private final JBCheckBox doNotShowConfirmation = new JBCheckBox("Do not show confirmation dialogs? ");
    private final JBCheckBox disableBranches = new JBCheckBox("Disable branches? ");
    private final JBCheckBox autoUpload = new JBCheckBox("Automatically upload on change? ");
    private final JBCheckBox disableCompletion = new JBCheckBox("Disable autocompletion? ");

    public SettingsPanel() {
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Login"))
                .addSeparator()
                .addLabeledComponent(new JBLabel("Project id *: "), projectId, 2)
                .addLabeledComponent(new JBLabel("Api token *: "), apiToken, 2)
                .addLabeledComponent(new JBLabel("Base URL: "), baseUrl, 2)
                .addComponent(new JBLabel("Settings"), 20)
                .addSeparator()
                .addComponent(doNotShowConfirmation, 5)
                .addComponent(disableBranches, 5)
                .addComponent(autoUpload, 5)
                .addComponent(disableCompletion, 5)
                .addLabeledComponent(new JBLabel("File extensions for autocompletion"), completionFileExtensions, 5)
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

    public void setDisableBranches(boolean disableBranches) {
        this.disableBranches.setSelected(disableBranches);
    }

    public boolean getDisableBranches() {
        return this.disableBranches.isSelected();
    }

    public void setAutoUpload(boolean autoUpload) {
        this.autoUpload.setSelected(autoUpload);
    }

    public boolean getAutoUpload() {
        return this.autoUpload.isSelected();
    }

    public void setDisableCompletion(boolean disableCompletion) {
        this.disableCompletion.setSelected(disableCompletion);
    }

    public boolean getDisableCompletion() {
        return this.disableCompletion.isSelected();
    }
}
