package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.ContextLogic;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.ActionUtils;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.UIUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Objects;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadSourceFromContextAction extends BackgroundAction {
    @Override
    protected void performInBackground(@NotNull AnActionEvent anActionEvent, @NotNull ProgressIndicator indicator) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        if (file == null) {
            return;
        }
        Project project = anActionEvent.getProject();
        try {
            VirtualFile root = FileUtil.getProjectBaseDir(project);

            CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

            boolean confirmation = UIUtil.confirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.download"), "Download");
            if (!confirmation) {
                return;
            }
            indicator.checkCanceled();

            CrowdinProperties properties;
            try {
                properties = CrowdinPropertiesLoader.load(project);
            } catch (Exception e) {
                NotificationUtil.showErrorMessage(project, e.getMessage());
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

            Long sourceId = ContextLogic.findSourceIdFromSourceFile(properties, crowdinProjectCache.getFileInfos(branch), file, root);
            URL url = crowdin.downloadFile(sourceId);
            FileUtil.downloadFile(this, file, url);
            NotificationUtil.showInformationMessage(project, MESSAGES_BUNDLE.getString("messages.success.download_source"));
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean isSourceFile = false;
        try {
            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            String branchName = ActionUtils.getBranchName(project, properties, false);
            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                    CrowdinProjectCacheProvider.getInstance(crowdin, branchName, false);

            //hide for SB
            isSourceFile = !crowdinProjectCache.isStringsBased() && properties.getFiles()
                .stream()
                .flatMap(fb -> FileUtil.getSourceFilesRec(FileUtil.getProjectBaseDir(project), fb.getSource()).stream())
                .anyMatch(f -> Objects.equals(file, f));
        } catch (Exception exception) {
//            do nothing
        } finally {
            e.getPresentation().setEnabled(isSourceFile);
            e.getPresentation().setVisible(isSourceFile);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.download_source_file_from_context"), (file != null ? file.getName() : "<UNKNOWN>"));
    }
}
