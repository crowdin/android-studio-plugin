package com.crowdin.ui.panel.upload.action;

import com.crowdin.action.BackgroundAction;
import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.FileBean;
import com.crowdin.logic.BranchLogic;
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
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

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
    protected void performInBackground(@NonNull AnActionEvent e, @NonNull ProgressIndicator indicator) {
        System.out.println("e.getProject() = " + e.getProject());
        boolean forceRefresh = !CrowdinPanelWindowFactory.PLACE_ID.equals(e.getPlace());
        Project project = e.getProject();
        e.getPresentation().setEnabled(false);
        isInProgress.set(true);
        try {
            UploadWindow window = ServiceManager
                    .getService(project, CrowdinPanelWindowFactory.ProjectService.class)
                    .getUploadWindow();

            if (window == null) {
                return;
            }

            VirtualFile root = FileUtil.getProjectBaseDir(project);

            CrowdinProperties properties;
            try {
                properties = CrowdinPropertiesLoader.load(project);
            } catch (Exception err) {
                NotificationUtil.showErrorMessage(project, err.getMessage());
                return;
            }

            NotificationUtil.setLogDebugLevel(properties.isDebug());
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

            Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            indicator.checkCanceled();

            BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
            String branchName = branchLogic.acquireBranchName(true);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                    CrowdinProjectCacheProvider.getInstance(crowdin, branchName, forceRefresh);

            List<String> files = new ArrayList<>();

            for (FileBean fileBean : properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(root, fileBean.getSource())) {
                    String file = Paths.get(root.getPath()).relativize(Paths.get(source.getPath())).toString();
                    files.add(file);
                }
            }

            ApplicationManager.getApplication()
                    .invokeAndWait(() -> window.rebuildTree(crowdinProjectCache.getProject().getName(), files));
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            if (project != null && forceRefresh) {
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
