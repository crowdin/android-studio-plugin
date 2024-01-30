package com.crowdin.ui.panel.download.action;

import com.crowdin.action.BackgroundAction;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class CollapseAction extends BackgroundAction {

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);

    public CollapseAction() {
        super("Collapse tree", "Collapse tree", AllIcons.Actions.Collapseall);
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
            DownloadWindow window = project.getService(ProjectService.class).getDownloadWindow();

            if (window == null) {
                return;
            }

            ApplicationManager.getApplication().invokeAndWait(window::collapseAll);
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
        return "Collapse download tree";
    }

}
