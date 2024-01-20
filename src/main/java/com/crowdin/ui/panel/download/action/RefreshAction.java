package com.crowdin.ui.panel.download.action;

import com.crowdin.action.BackgroundAction;
import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.FileBean;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.logic.BranchLogic;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.download.DownloadWindow;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

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
            DownloadWindow window = ServiceManager.getService(project, CrowdinPanelWindowFactory.ProjectService.class)
                    .getDownloadWindow();
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

            BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
            String branchName = branchLogic.acquireBranchName(true);
            indicator.checkCanceled();

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                    CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = branchLogic.getBranch(crowdinProjectCache, false);

            Map<String, FileInfo> filePaths = crowdinProjectCache.getFileInfos(branch);

            NotificationUtil.logDebugMessage(project, "Project files: " + filePaths.keySet());

            List<String> files = new ArrayList<>();

            //copy from Download action
            for (FileBean fileBean : properties.getFiles()) {
                Predicate<String> sourcePredicate = FileUtil.filePathRegex(fileBean.getSource(), properties.isPreserveHierarchy());
                Map<String, VirtualFile> localSourceFiles = (properties.isPreserveHierarchy())
                        ? Collections.emptyMap()
                        : FileUtil.getSourceFilesRec(root, fileBean.getSource()).stream()
                        .collect(Collectors.toMap(VirtualFile::getPath, Function.identity()));
                List<String> foundSources = filePaths.keySet().stream()
                        .map(FileUtil::unixPath)
                        .filter(sourcePredicate)
                        .map(FileUtil::normalizePath)
                        .sorted()
                        .collect(Collectors.toList());

                for (String foundSourceFilePath : foundSources) {
                    if (!properties.isPreserveHierarchy()) {
                        List<String> fittingSources = localSourceFiles.keySet().stream()
                                .filter(localSourceFilePath -> localSourceFilePath.endsWith(foundSourceFilePath))
                                .collect(Collectors.toList());
                        if (fittingSources.isEmpty()) {
                            //no representative local source files
                            continue;
                        } else if (fittingSources.size() > 1) {
                            //more than one file that can be representative
                            continue;
                        }
                    }
                    String file = Paths.get(root.getPath()).relativize(Paths.get(foundSourceFilePath)).toString();
                    files.add(file);
                }
            }

            ApplicationManager.getApplication()
                    .invokeAndWait(() -> window.rebuildTree(crowdinProjectCache.getProject().getName(), files));
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
        return "Refresh download panel";
    }

}
