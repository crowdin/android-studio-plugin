package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DownloadAction extends BackgroundAction {

    @Override
    public void performInBackground(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = project.getBaseDir();
        CrowdinProperties properties;
        try {
            properties = CrowdinPropertiesLoader.load(project);
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
            return;
        }
        Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
        String branch = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

        File downloadTranslations = crowdin.downloadTranslations(virtualFile, branch);
        if (downloadTranslations == null) {
            return;
        }
        String tempDir = downloadTranslations.getParent() + File.separator + "all" + System.nanoTime() + File.separator;
        this.extractTranslations(project, downloadTranslations, tempDir);
        List<String> files = FileUtil.walkDir(Paths.get(tempDir)).stream()
            .map(File::getAbsolutePath)
            .map(path -> StringUtils.removeStart(path, tempDir))
            .map(f -> f.replaceAll("[\\\\/]+", "/"))
            .collect(Collectors.toList());

        List<Language> projectLangs = crowdin.getProjectLanguages();

        properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
            List<VirtualFile> sources = FileUtil.getSourceFilesRec(virtualFile, sourcePattern);
            sources.forEach(source -> {
                VirtualFile parent = FileUtil.getBaseDir(source, sourcePattern);
                String basePattern = PlaceholderUtil.replaceFilePlaceholders(
                    translationPattern,
                    StringUtils.removeStart(source.getPath(), virtualFile.getPath()));
                projectLangs.forEach(projectLanguage -> {
                    String languageBasedPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, projectLanguage);
                    languageBasedPattern = StringUtils.removeStart(languageBasedPattern, "/");
                    if (!files.contains(languageBasedPattern)) {
                        return;
                    }
                    File fromFile = new File(tempDir + languageBasedPattern);
                    File toFile = new File(parent.getPath() + File.separator + languageBasedPattern);
                    toFile.getParentFile().mkdirs();
                    if (!fromFile.renameTo(toFile) && toFile.delete() && !fromFile.renameTo(toFile)) {
                        NotificationUtil.showWarningMessage(project, "Failed to extract file '" + toFile + "'.");
                    }
                });
            });
        });
        if (downloadTranslations.delete()) {
            System.out.println("all.zip was deleted");
        } else {
            System.out.println("all.zip wasn't deleted");
        }
        try {
            FileUtils.deleteDirectory(new File(tempDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        virtualFile.refresh(true, true);
        NotificationUtil.showInformationMessage(project, "Translations successfully downloaded");
    }

    @Override
    String loadingText(AnActionEvent e) {
        return "Downloading Translations";
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
            NotificationUtil.showInformationMessage(project, "Downloading translations failed");
            e.printStackTrace();
        }
    }
}
