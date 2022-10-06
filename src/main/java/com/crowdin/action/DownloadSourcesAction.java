package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.FileBean;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.UIUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadSourcesAction extends BackgroundAction {

    @Override
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text_download_sources");
    }

    @Override
    protected void performInBackground(@NonNull AnActionEvent anActionEvent, @NonNull ProgressIndicator indicator) {
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

            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
            String branchName = branchLogic.acquireBranchName(true);
            indicator.checkCanceled();

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = branchLogic.getBranch(crowdinProjectCache, false);

            Map<String, FileInfo> filePaths = crowdinProjectCache.getFileInfos(branch);

            AtomicBoolean isAnyFileDownloaded = new AtomicBoolean(false);

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

                if (foundSources.isEmpty()) {
                    NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.no_sources_for_pattern"), fileBean.getSource()));
                    return;
                }
                for (String foundSourceFilePath : foundSources) {
                    if (properties.isPreserveHierarchy()) {
                        Long fileId = filePaths.get(foundSourceFilePath).getId();
                        this.downloadFile(crowdin, fileId, root, foundSourceFilePath);
                        isAnyFileDownloaded.set(true);
                    } else {
                        List<String> fittingSources = localSourceFiles.keySet().stream()
                            .filter(localSourceFilePath -> localSourceFilePath.endsWith(foundSourceFilePath))
                            .collect(Collectors.toList());
                        if (fittingSources.isEmpty()) {
                            NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.file_no_representative"), foundSourceFilePath));
                            continue;
                        } else if (fittingSources.size() > 1) {
                            NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.file_not_one_representative"), foundSourceFilePath));
                            continue;
                        }
                        Long fileId = filePaths.get(foundSourceFilePath).getId();
                        VirtualFile file = localSourceFiles.get(fittingSources.get(0));
                        this.downloadFile(crowdin, fileId, file);
                        isAnyFileDownloaded.set(true);
                    }
                    NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download_sources.file_downloaded"), foundSourceFilePath));
                }
            }

            if (isAnyFileDownloaded.get()) {
                NotificationUtil.showInformationMessage(project, MESSAGES_BUNDLE.getString("messages.success.download_sources"));
            } else {
                NotificationUtil.showWarningMessage(project, MESSAGES_BUNDLE.getString("messages.failure.download_sources"));
            }

        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    private void downloadFile(Crowdin client, Long fileId, VirtualFile root, String filePath) {
        URL url = client.downloadFile(fileId);
        try (InputStream data = url.openStream()) {
            VirtualFile file = FileUtil.createIfNeededFilePath(this, root, filePath);
            FileUtil.downloadFile(this, file, data);
        } catch (IOException e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.download_file"), filePath, e.getMessage()), e);
        }
    }

    private void downloadFile(Crowdin client, Long fileId, VirtualFile file) {
        URL url = client.downloadFile(fileId);
        try (InputStream data = url.openStream()) {
            FileUtil.downloadFile(this, file, data);
        } catch (IOException e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.download_file"), file.getPath(), e.getMessage()), e);
        }
    }
}
