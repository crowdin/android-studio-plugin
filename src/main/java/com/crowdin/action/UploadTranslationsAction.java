package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class UploadTranslationsAction extends BackgroundAction {

    @Override
    public void performInBackground(@NotNull AnActionEvent e, ProgressIndicator indicator) {
        Project project = e.getProject();
        VirtualFile root = FileUtil.getProjectBaseDir(project);

        CrowdinProperties properties;
        try {
            CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

            boolean confirmation = UIUtil.сonfirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.upload_translations"), "Upload");
            if (!confirmation) {
                return;
            }
            indicator.checkCanceled();

            properties = CrowdinPropertiesLoader.load(project);
            NotificationUtil.setLogDebugLevel(properties.isDebug());
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            indicator.checkCanceled();

            BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
            String branchName = branchLogic.acquireBranchName(true);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            if (!crowdinProjectCache.isManagerAccess()) {
                NotificationUtil.showErrorMessage(project, "You need to have manager access to perform this action");
                return;
            }

            Branch branch = branchLogic.getBranch(crowdinProjectCache, false);

            Map<String, FileInfo> filePaths = crowdinProjectCache.getFileInfos(branch);

            NotificationUtil.logDebugMessage(project, "Project files: " + filePaths.keySet());

            AtomicInteger uploadedFilesCounter = new AtomicInteger(0);

            for (FileBean fileBean : properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(root, fileBean.getSource())) {
                    VirtualFile pathToPattern = FileUtil.getBaseDir(source, fileBean.getSource());
                    String sourceRelativePath = properties.isPreserveHierarchy() ? StringUtils.removeStart(source.getPath(), root.getPath()) : FileUtil.sepAtStart(source.getName());

                    Map<Language, String> translationPaths =
                        PlaceholderUtil.buildTranslationPatterns(sourceRelativePath, fileBean.getTranslation(), crowdinProjectCache.getProjectLanguages(), crowdinProjectCache.getLanguageMapping());

                    FileInfo crowdinSource = filePaths.get(FileUtil.normalizePath(sourceRelativePath));
                    if (crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_source"), FileUtil.normalizePath((branchName != null ? branchName + "/" : "") + sourceRelativePath)));
                        return;
                    }
                    for (Map.Entry<Language, String> translationPath : translationPaths.entrySet()) {
                        java.io.File translationFile = Paths.get(pathToPattern.getPath(), translationPath.getValue()).toFile();
                        if (!translationFile.exists()) {
                            NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_translation"), FileUtil.noSepAtStart(StringUtils.removeStart(translationFile.getPath(), root.getPath()))));
                            continue;
                        }
                        Long storageId;
                        try (InputStream translationFileStrem = new FileInputStream(translationFile)) {
                            storageId = crowdin.addStorage(translationFile.getName(), translationFileStrem);
                        } catch (IOException exception) {
                            throw new RuntimeException("Unhandled exception with File '" + translationFile + "'", exception);
                        }
                        UploadTranslationsRequest request = RequestBuilder.uploadTranslation(crowdinSource.getId(), storageId);
                        try {
                            crowdin.uploadTranslation(translationPath.getKey().getId(), request);
                            uploadedFilesCounter.incrementAndGet();
                        } catch (Exception exception) {
                            NotificationUtil.logErrorMessage(project, exception);
                            NotificationUtil.showErrorMessage(project, "Couldn't upload translation file '" + translationFile + "': " + exception.getMessage());
                        }
                    }
                }
            }
            if (uploadedFilesCounter.get() > 0) {
                NotificationUtil.showInformationMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translations"), uploadedFilesCounter.get()));
            } else {
                NotificationUtil.showWarningMessage(project, MESSAGES_BUNDLE.getString("errors.uploaded_zero_translations"));
            }
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (Exception exception) {
            NotificationUtil.logErrorMessage(project, exception);
            NotificationUtil.showErrorMessage(project, exception.getMessage());
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_translations");
    }
}
