package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadFromContextAction extends BackgroundAction {
    @Override
    public void performInBackground(AnActionEvent anActionEvent, ProgressIndicator indicator) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        Project project = anActionEvent.getProject();
        try {
            CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

            boolean confirmation = UIUtil.сonfirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.upload_source_file"), "Upload");
            if (!confirmation) {
                return;
            }
            indicator.checkCanceled();

            VirtualFile root = FileUtil.getProjectBaseDir(project);

            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            NotificationUtil.setLogDebugLevel(properties.isDebug());
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            String branchName = ActionUtils.getBranchName(project, properties, true);
            indicator.checkCanceled();

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = crowdinProjectCache.getBranches().get(branchName);
            if (branch == null && StringUtils.isNotEmpty(branchName)) {
                AddBranchRequest addBranchRequest = RequestBuilder.addBranch(branchName);
                branch = crowdin.addBranch(addBranchRequest);
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.created_branch"), branch.getId(), branch.getName()));
            } else if (branch != null) {
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.using_branch"), branch.getId(), branch.getName()));
            }
            indicator.checkCanceled();

            FileBean foundFileBean = properties.getFiles()
                .stream()
                .filter(fb -> FileUtil.getSourceFilesRec(root, fb.getSource()).contains(file))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Unexpected error: couldn't find suitable source pattern"));

            indicator.checkCanceled();

            Map<FileBean, List<VirtualFile>> source = Collections.singletonMap(foundFileBean, Collections.singletonList(file));
            SourceLogic.processSources(project, root, crowdin, crowdinProjectCache, branch, properties.isPreserveHierarchy(), source);

            CrowdinProjectCacheProvider.outdateBranch(branchName);
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
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean isSourceFile = false;
        try {
            CrowdinProperties properties;
            properties = CrowdinPropertiesLoader.load(project);
            List<VirtualFile> files = properties.getFiles()
                .stream()
                .flatMap(fb -> FileUtil.getSourceFilesRec(FileUtil.getProjectBaseDir(project), fb.getSource()).stream())
                .collect(Collectors.toList());
            isSourceFile = files.contains(file);
        } catch (Exception exception) {
//            do nothing
        } finally {
            e.getPresentation().setEnabled(isSourceFile);
            e.getPresentation().setVisible(isSourceFile);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources_from_context"), CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName());
    }
}
