package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
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
            properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            Branch branch = crowdinProjectCache.getBranches().get(branchName);
            if (branch == null) {
                NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.branch_not_exists"),  branchName));
            }

            Map<String, File> filePaths = crowdinProjectCache.getFiles().get(branch);

            AtomicInteger uploadedFilesCounter = new AtomicInteger(0);

            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
                sources.forEach(source -> {
                    VirtualFile baseDir = FileUtil.getBaseDir(source, sourcePattern);
                    String sourcePath = source.getName();

                    File crowdinSource = filePaths.get(sourcePath);
                    if (crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_source"), (branchName != null ? branchName + "/" : "") + sourcePath));
                        return;
                    }
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(translationPattern, sourcePath);
                    for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang);
                        java.io.File translationFile = Paths.get(baseDir.getPath(), builtPattern).toFile();
                        if (!translationFile.exists()) {
                            continue;
                        }
                        boolean uploaded = crowdin.uploadTranslationFile(translationFile, crowdinSource.getId(), lang.getId());
                        if (uploaded) {
                            uploadedFilesCounter.incrementAndGet();
                        }
                    }
                });
            });
            NotificationUtil.showInformationMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translations"), uploadedFilesCounter.get()));
        } catch (Exception exception) {
            NotificationUtil.showErrorMessage(project, exception.getMessage());
            return;
        }
    }

    @Override
    String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_translations");
    }
}
