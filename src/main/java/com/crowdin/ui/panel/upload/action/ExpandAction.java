package com.crowdin.ui.panel.upload.action;

import com.crowdin.action.BackgroundAction;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.crowdin.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExpandAction extends BackgroundAction {

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);

    public ExpandAction() {
        super("Expand tree", "Expand tree", AllIcons.Actions.Expandall);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!isInProgress.get());
    }

    @Override
    protected void performInBackground(@NotNull AnActionEvent e, @NotNull ProgressIndicator indicator) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        e.getPresentation().setEnabled(false);
        isInProgress.set(true);
        try {
            UploadWindow window = project.getService(ProjectService.class).getUploadWindow();

            if (window == null) {
                return;
            }

            ApplicationManager.getApplication().invokeAndWait(window::expandAll);
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            NotificationUtil.logErrorMessage(project, ex);
            NotificationUtil.showErrorMessage(project, ex.getMessage());
        } finally {
            e.getPresentation().setEnabled(true);
            isInProgress.set(false);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return "Expand upload tree";
    }

}
