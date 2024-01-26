package com.crowdin.ui.panel.upload.action;

import com.crowdin.action.ActionContext;
import com.crowdin.action.BackgroundAction;
import com.crowdin.client.FileBean;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends BackgroundAction {

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);

    public RefreshAction() {
        super("Refresh data", "Refresh data", AllIcons.Actions.Refresh);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!isInProgress.get());
    }

    @Override
    protected void performInBackground(@NotNull AnActionEvent e, @NotNull ProgressIndicator indicator) {
        boolean forceRefresh = !CrowdinPanelWindowFactory.PLACE_ID.equals(e.getPlace());
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        e.getPresentation().setEnabled(false);
        isInProgress.set(true);
        try {
            UploadWindow window = ServiceManager
                    .getService(project, CrowdinPanelWindowFactory.ProjectService.class)
                    .getUploadWindow();

            if (window == null) {
                return;
            }

            Optional<ActionContext> context = super.prepare(project, indicator, false, false, forceRefresh, null, null);

            if (context.isEmpty()) {
                return;
            }

            List<String> files = new ArrayList<>();

            for (FileBean fileBean : context.get().properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource())) {
                    String file = Paths.get(context.get().root.getPath()).relativize(Paths.get(source.getPath())).toString();
                    files.add(file);
                }
            }

            ApplicationManager.getApplication()
                    .invokeAndWait(() -> window.rebuildTree(context.get().crowdinProjectCache.getProject().getName(), files));
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            if (forceRefresh) {
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
