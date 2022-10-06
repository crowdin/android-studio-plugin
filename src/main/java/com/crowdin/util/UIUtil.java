package com.crowdin.util;

import com.crowdin.logic.CrowdinSettings;
import com.crowdin.ui.ConfirmActionDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.util.concurrent.atomic.AtomicReference;

public class UIUtil {

    public static boolean confirmDialog(Project project, CrowdinSettings settings, String questionText, String okButtonText) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            return true;
        }
        if (!settings.isDoNotShowConfirms()) {
            AtomicReference<Boolean> confirmation = new AtomicReference<>();
            ApplicationManager.getApplication().invokeAndWait(() -> {
                ConfirmActionDialog confirmDialog =
                    new ConfirmActionDialog(project, questionText, okButtonText);
                confirmation.set(confirmDialog.showAndGet());
                settings.setDoNotShowConfirms(confirmDialog.isDoNotAskAgain());
            });
            return confirmation.get();
        } else {
            return true;
        }

    }
}