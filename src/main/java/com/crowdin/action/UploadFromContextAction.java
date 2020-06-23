package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.UIUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
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

            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);
            indicator.checkCanceled();

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = crowdinProjectCache.getBranches().get(branchName);
            if (branch == null && StringUtils.isNotEmpty(branchName)) {
                AddBranchRequest addBranchRequest = RequestBuilder.addBranch(branchName);
                branch = crowdin.addBranch(addBranchRequest);
            }
            Long branchId = (branch != null) ? branch.getId() : null;
            indicator.checkCanceled();

            Map<String, File> filePaths = crowdinProjectCache.getFiles().getOrDefault(branch, new HashMap<>());
            Map<String, Directory> dirPaths = crowdinProjectCache.getDirs().getOrDefault(branch, new HashMap<>());

            String sourcePattern = properties.getSourcesWithPatterns().keySet()
                .stream()
                .filter(s -> FileUtil.getSourceFilesRec(project.getBaseDir(), s).contains(file))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Unexpected error: couldn't find suitable source pattern"));
            String translationPattern = properties.getSourcesWithPatterns().get(sourcePattern);

            indicator.checkCanceled();
            SourceLogic sourceLogic = new SourceLogic(project, crowdin, properties, filePaths, dirPaths, branchId);
            sourceLogic.uploadSource(file, sourcePattern, translationPattern);

            CrowdinProjectCacheProvider.outdateBranch(branchName);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
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
            List<VirtualFile> files = properties.getSourcesWithPatterns().keySet()
                .stream()
                .flatMap(s -> FileUtil.getSourceFilesRec(project.getBaseDir(), s).stream())
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
    String loadingText(AnActionEvent e) {
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources_from_context"), CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName());
    }
}
