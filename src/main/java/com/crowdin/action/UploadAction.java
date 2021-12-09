package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/10/17.
 */
@SuppressWarnings("ALL")
public class UploadAction extends BackgroundAction {
    @Override
    public void performInBackground(@NotNull final AnActionEvent anActionEvent, ProgressIndicator indicator) {
        Project project = anActionEvent.getProject();
        try {
            CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

            boolean confirmation = UIUtil.сonfirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.upload_sources"), "Upload");
            if (!confirmation) {
                return;
            }
            indicator.checkCanceled();

            VirtualFile root = FileUtil.getProjectBaseDir(project);

            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            NotificationUtil.setLogDebugLevel(properties.isDebug());
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.upload_sources.list_of_patterns")
                + properties.getFiles().stream()
                .map(fileBean -> String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.list_of_patterns_item"), fileBean.getSource(), fileBean.getTranslation()))
                .collect(Collectors.joining()));

            BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
            String branchName = branchLogic.acquireBranchName(true);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);
            indicator.checkCanceled();

            Branch branch = branchLogic.getBranch(crowdinProjectCache, true);
            indicator.checkCanceled();

            Map<FileBean, List<VirtualFile>> sources = properties.getFiles().stream()
                .collect(Collectors.toMap(Function.identity(), fileBean -> FileUtil.getSourceFilesRec(root, fileBean.getSource())));
            SourceLogic.processSources(project, root, crowdin, crowdinProjectCache, branch, properties.isPreserveHierarchy(), sources);
            CrowdinProjectCacheProvider.outdateBranch(branchName);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources");
    }
}
