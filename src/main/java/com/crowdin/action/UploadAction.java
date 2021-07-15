package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                + properties.getSourcesWithPatterns().keySet().stream()
                .map(key -> String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.list_of_patterns_item"), key, properties.getSourcesWithPatterns().get(key)))
                .collect(Collectors.joining()));

            String branchName = ActionUtils.getBranchName(project, properties, true);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);
            indicator.checkCanceled();

            Branch branch = crowdinProjectCache.getBranches().get(branchName);
            if (branch == null && StringUtils.isNotEmpty(branchName)) {
                AddBranchRequest addBranchRequest = RequestBuilder.addBranch(branchName);
                branch = crowdin.addBranch(addBranchRequest);
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.created_branch"), branch.getId(), branch.getName()));
            } else if (branch != null) {
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.using_branch"), branch.getId(), branch.getName()));
            }
            indicator.checkCanceled();

            Map<String, FileInfo> filePaths = crowdinProjectCache.getFileInfos(branch);
            Map<String, Directory> dirPaths = crowdinProjectCache.getDirs().getOrDefault(branch, new HashMap<>());
            Long branchId = (branch != null) ? branch.getId() : null;

            SourceLogic sourceLogic = new SourceLogic(project, crowdin, properties, filePaths, dirPaths, branchId);

            indicator.checkCanceled();

            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sourceFiles = FileUtil.getSourceFilesRec(root, sourcePattern);
                sourceFiles.forEach(sf -> {
                    try {
                        sourceLogic.uploadSource(sf, sourcePattern, translationPattern);
                    } catch (Exception e) {
                        NotificationUtil.logErrorMessage(project, e);
                        NotificationUtil.showErrorMessage(project, e.getMessage());
                    }
                });
            });
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
