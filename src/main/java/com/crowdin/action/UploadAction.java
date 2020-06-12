package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/10/17.
 */
@SuppressWarnings("ALL")
public class UploadAction extends BackgroundAction {
    @Override
    public void performInBackground(@NotNull final AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        try {
            VirtualFile root = project.getBaseDir();

            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = crowdinProjectCache.getBranches().get(branchName);
            if (branch == null && StringUtils.isNotEmpty(branchName)) {
                AddBranchRequest addBranchRequest = RequestBuilder.addBranch(branchName);
                branch = crowdin.addBranch(addBranchRequest);
            }

            Map<String, File> filePaths = crowdinProjectCache.getFiles().getOrDefault(branch, new HashMap<>());
            Map<String, Directory> dirPaths = crowdinProjectCache.getDirs().getOrDefault(branch, new HashMap<>());
            Long branchId = (branch != null) ? branch.getId() : null;

            SourceLogic sourceLogic = new SourceLogic(project, crowdin, properties, filePaths, dirPaths, branchId);

            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sourceFiles = FileUtil.getSourceFilesRec(root, sourcePattern);
                sourceFiles.forEach(sf -> {
                    try {
                        sourceLogic.uploadSource(sf, sourcePattern, translationPattern);
                    } catch (Exception e) {
                        NotificationUtil.showErrorMessage(project, e.getMessage());
                    }
                });
            });
            CrowdinProjectCacheProvider.outdateBranch(branchName);
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    @Override
    String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources");
    }
}
