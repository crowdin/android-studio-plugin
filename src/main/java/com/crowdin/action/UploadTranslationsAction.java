package com.crowdin.action;

import com.crowdin.client.Crowdin;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UploadTranslationsAction extends BackgroundAction {

    @Override
    public void performInBackground(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile root = project.getBaseDir();

        CrowdinProperties properties;
        try {
            properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            List<Language> projectLanguages = crowdin.getProjectLanguages();

            String branch = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);
            Long branchId = crowdin.getBranch(branch).map(Branch::getId).orElse(null);

            List<com.crowdin.client.sourcefiles.model.File> files = crowdin.getFiles(branchId);
            Map<Long, Directory> dirs = crowdin.getDirectories(branchId);
            Map<String, File> filePaths = CrowdinFileUtil.buildFilePaths(files, dirs);

            AtomicInteger uploadedFilesCounter = new AtomicInteger(0);

            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
                sources.forEach(source -> {
                    VirtualFile baseDir = FileUtil.getBaseDir(source, sourcePattern);
                    String sourcePath = source.getName();

                    File crowdinSource = filePaths.get(sourcePath);
                    if (crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, "File '" + (branch != null ? branch + "/" : "") + sourcePath + "' is missing in the project. Run 'Upload' to upload the missing source");
                        return;
                    }
                    String pattern1 = PlaceholderUtil.replaceFilePlaceholders(translationPattern, sourcePath);
                    for (Language lang : projectLanguages) {
                        String pattern2 = PlaceholderUtil.replaceLanguagePlaceholders(pattern1, lang);
                        java.io.File translationFile = new java.io.File(baseDir.getPath() + "/" + pattern2);
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
            NotificationUtil.showInformationMessage(project, "Uploaded " + uploadedFilesCounter.get() + " files");
        } catch (Exception exception) {
            NotificationUtil.showErrorMessage(project, exception.getMessage());
            return;
        }
    }

    @Override
    String loadingText(AnActionEvent e) {
        return "Uploading Translations";
    }
}
