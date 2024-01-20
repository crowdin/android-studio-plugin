package com.crowdin.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ConfirmActionDialog extends DialogWrapper {

    ConfirmActionPanel confirmActionPanel;

    public ConfirmActionDialog(Project project, String questionText, String OKButtonText) {
        super(project, true);
        confirmActionPanel = new ConfirmActionPanel(questionText);
        setTitle("Crowdin");
        setOKButtonText(OKButtonText);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return confirmActionPanel.getPanel();
    }

    public Boolean isDoNotAskAgain() {
        return confirmActionPanel.isDoNotAskAgain();
    };
}
