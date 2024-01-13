package com.crowdin.ui.panel.upload.action;

import com.crowdin.action.BackgroundAction;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.crowdin.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends BackgroundAction {

    private AtomicBoolean isInProgress = new AtomicBoolean(false);

    public RefreshAction() {
        super("Refresh data", "Refresh data", AllIcons.Actions.Refresh);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!isInProgress.get());
    }

    @Override
    protected void performInBackground(@NonNull AnActionEvent e, @NonNull ProgressIndicator indicator) {
        System.out.println("e.getProject() = " + e.getProject());
        Project project = e.getProject();
        e.getPresentation().setEnabled(false);
        isInProgress.set(true);
        try {
            UploadWindow window = ServiceManager.getService(project, CrowdinPanelWindowFactory.ProjectService.class)
                    .getUploadWindow();
            if (window == null) {
                return;
            }

            System.out.println("Refresh Upload Window");
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            if (project != null) {
                NotificationUtil.logErrorMessage(project, ex);
                NotificationUtil.showErrorMessage(project, ex.getMessage());
            }
        } finally {
            e.getPresentation().setEnabled(true);
            isInProgress.set(false);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return "Refresh upload panel";
    }

}
