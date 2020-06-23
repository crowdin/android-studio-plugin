package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class UploadTranslationsAction extends BackgroundAction {

    @Override
    public void performInBackground(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile root = project.getBaseDir();

        CrowdinProperties properties;
        try {
            CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

            boolean confirmation = UIUtil.сonfirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.upload_translations"), "Upload");
            if (!confirmation) {
                return;
            }

            properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = crowdinProjectCache.getBranches().get(branchName);
            if ((branchName != null && !branchName.isEmpty()) && branch == null) {
                NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.branch_not_exists"),  branchName));
                return;
            }

            Map<String, File> filePaths = crowdinProjectCache.getFiles().getOrDefault(branch, new HashMap<>());

            AtomicInteger uploadedFilesCounter = new AtomicInteger(0);

            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
                sources.forEach(source -> {
                    VirtualFile pathToPattern = FileUtil.getBaseDir(source, sourcePattern);

                    String relativePathToPattern = (properties.isPreserveHierarchy())
                        ? java.io.File.separator + VfsUtil.findRelativePath(root, pathToPattern, java.io.File.separatorChar)
                        : "";
                    String patternPathToFile = (properties.isPreserveHierarchy())
                        ? java.io.File.separator + VfsUtil.findRelativePath(pathToPattern, source.getParent(), java.io.File.separatorChar)
                        : "";

                    File crowdinSource = filePaths.get(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName()));
                    if (crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_source"), (branchName != null ? branchName + "/" : "") + FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName())));
                        return;
                    }
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(translationPattern, FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName()));
                    for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang);
                        java.io.File translationFile = Paths.get(pathToPattern.getPath(), builtPattern).toFile();
                        if (!translationFile.exists()) {
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
                            crowdin.uploadTranslation(lang.getId(), request);
                            uploadedFilesCounter.incrementAndGet();
                        } catch (Exception exception) {
                            NotificationUtil.showErrorMessage(project, "Couldn't upload translation file '" + translationFile + "': " + exception.getMessage());
                        }
                    }
                });
            });
            if (uploadedFilesCounter.get() > 0) {
                NotificationUtil.showInformationMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translations"), uploadedFilesCounter.get()));
            }
        } catch (Exception exception) {
            NotificationUtil.showErrorMessage(project, exception.getMessage());
        }
    }

    @Override
    String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_translations");
    }
}
