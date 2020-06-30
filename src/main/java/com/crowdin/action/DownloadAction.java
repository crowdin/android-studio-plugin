package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadAction extends BackgroundAction {

    @Override
    public void performInBackground(AnActionEvent anActionEvent, ProgressIndicator indicator) {
        Project project = anActionEvent.getProject();
        VirtualFile root = FileUtil.getProjectBaseDir(project);

        CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

        boolean confirmation = UIUtil.сonfirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.download"), "Download");
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
        Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
        String branch = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);
        indicator.checkCanceled();

        File downloadTranslations = crowdin.downloadTranslations(root, branch);
        if (downloadTranslations == null) {
            return;
        }
        String tempDir = downloadTranslations.getParent() + File.separator + "all" + System.nanoTime();
        this.extractTranslations(project, downloadTranslations, tempDir);
        List<String> files = FileUtil.walkDir(Paths.get(tempDir)).stream()
            .map(File::getAbsolutePath)
            .map(path -> StringUtils.removeStart(path, tempDir))
            .map(FileUtil::normalizePath)
            .collect(Collectors.toList());

        List<Language> projectLangs = crowdin.getProjectLanguages();

        properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
            List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
            sources.forEach(source -> {
                VirtualFile pathToPattern = FileUtil.getBaseDir(source, sourcePattern);
                String relativePathToPattern = (properties.isPreserveHierarchy())
                    ? File.separator + FileUtil.findRelativePath(root, pathToPattern)
                    : File.separator;
                String basePattern = PlaceholderUtil.replaceFilePlaceholders(
                    translationPattern,
                    StringUtils.removeStart(source.getPath(), root.getPath()));
                projectLangs.forEach(projectLanguage -> {
                    String languageBasedPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, projectLanguage);
                    if (!files.contains(FileUtil.joinPaths(relativePathToPattern, languageBasedPattern))) {
                        return;
                    }
                    File fromFile = new File(FileUtil.joinPaths(tempDir, relativePathToPattern, languageBasedPattern));
                    File toFile = new File(FileUtil.joinPaths(pathToPattern.getPath(), languageBasedPattern));
                    toFile.getParentFile().mkdirs();
                    if (!fromFile.renameTo(toFile) && toFile.delete() && !fromFile.renameTo(toFile)) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.extract_file"), toFile));
                    }
                });
            });
        });
        downloadTranslations.delete();
        try {
            FileUtils.deleteDirectory(new File(tempDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        root.refresh(true, true);
        NotificationUtil.showInformationMessage(project, MESSAGES_BUNDLE.getString("messages.success.download"));
    }

    @Override
    String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.download");
    }

    private void extractTranslations(Project project, File archive, String dirPath) {
        if (archive == null) {
            return;
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(archive);
        } catch (ZipException e) {
            e.printStackTrace();
        }

        try {
            zipFile.extractAll(dirPath);
        } catch (ZipException e) {
            NotificationUtil.showInformationMessage(project, MESSAGES_BUNDLE.getString("errors.extract_archive"));
            e.printStackTrace();
        }
    }
}
