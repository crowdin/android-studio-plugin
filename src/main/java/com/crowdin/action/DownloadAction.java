package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
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
        NotificationUtil.setLogDebugLevel(properties.isDebug());
        NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

        Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
        String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

        if (!CrowdinFileUtil.isValidBranchName(branchName)) {
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.branch_contains_forbidden_symbols"));
        }
        indicator.checkCanceled();

        CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
            CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

        Branch branch = null;
        if (branchName != null && branchName.length() > 0) {
            Optional<Branch> foundBranch = crowdin.getBranch(branchName);
            if (!foundBranch.isPresent()) {
                NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.branch_not_exists"), branchName));
                return;
            } else {
                branch = foundBranch.get();
            }
            NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.using_branch"), branch.getId(), branch.getName()));
        }

        Map<String, String> allCrowdinTranslationsWithSources = CrowdinFileUtil.buildAllProjectTranslationsWithSources(
            new ArrayList<>(crowdinProjectCache.getFiles().getOrDefault(branch, new HashMap<>()).values()),
            CrowdinFileUtil.revDirPaths(crowdinProjectCache.getDirs().getOrDefault(branch, new HashMap<>())),
            crowdinProjectCache.getProjectLanguages()
        );

        NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.download.download_archive"));
        File downloadTranslations = crowdin.downloadTranslations(root, (branch != null ? branch.getId() : null));
        if (downloadTranslations == null) {
            return;
        }
        String tempDir = downloadTranslations.getParent() + File.separator + "all" + System.nanoTime();
        NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.extract_files"), tempDir));
        this.extractTranslations(project, downloadTranslations, tempDir);
        List<java.io.File> files = FileUtil.walkDir(Paths.get(tempDir));

        List<Language> projectLangs = crowdin.getProjectLanguages();

        List<Pair<File, File>> targets = new ArrayList<>();
        properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
            List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
            for (VirtualFile source : sources) {
                VirtualFile pathToPattern = FileUtil.getBaseDir(source, sourcePattern);
                String sourceRelativePath = StringUtils.removeStart(source.getPath(), root.getPath());
                String relativePathToPattern = (properties.isPreserveHierarchy())
                    ? File.separator + FileUtil.findRelativePath(root, pathToPattern)
                    : File.separator;
                Map<Language, String> translationPaths =
                    PlaceholderUtil.buildTranslationPatterns(sourceRelativePath, translationPattern, projectLangs);
                for (Map.Entry<Language, String> translationPathEntry : translationPaths.entrySet()) {
                    File fromFile = new File(FileUtil.joinPaths(tempDir, relativePathToPattern, translationPathEntry.getValue()));
                    File toFile = new File(FileUtil.joinPaths(pathToPattern.getPath(), translationPathEntry.getValue()));
                    if (!files.contains(fromFile)) {
                        NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.file_not_found"),
                            FileUtil.joinPaths(relativePathToPattern, translationPathEntry.getValue())));
                        return;
                    }
                    NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.file_found"),
                        FileUtil.joinPaths(relativePathToPattern, translationPathEntry.getValue())));
                    targets.add(Pair.create(fromFile, toFile));
                }
            }
        });
        for (Pair<File, File> target : targets) {
            File fromFile = target.first;
            File toFile = target.second;
            toFile.getParentFile().mkdirs();
            if (!fromFile.renameTo(toFile) && toFile.delete() && !fromFile.renameTo(toFile)) {
                NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.extract_file"), toFile));
            }
        }

        List<File> foundTranslations = targets.stream().map(p -> p.getFirst()).collect(Collectors.toList());
        List<File> omittedTranslations = files.stream()
            .filter(file -> !foundTranslations.contains(file))
            .collect(Collectors.toList());

        NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.download.clearing"));
        downloadTranslations.delete();
        try {
            FileUtils.deleteDirectory(new File(tempDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        root.refresh(true, true);

        Set<String> omittedSources = new HashSet<>();
        Set<String> notFoundTranslations = new HashSet<>();
        if (!omittedTranslations.isEmpty()) {
            for (File omittedTranslation : omittedTranslations) {
                String omittedFileString = StringUtils.removeStart(omittedTranslation.toString(), tempDir);
                if (allCrowdinTranslationsWithSources.containsKey(omittedFileString)) {
                    omittedSources.add(allCrowdinTranslationsWithSources.get(omittedFileString));
                } else {
                    notFoundTranslations.add(omittedFileString);
                }
            }
        }

        if (!omittedSources.isEmpty() || !notFoundTranslations.isEmpty()) {
            String omittedSourcesText = omittedSources.isEmpty()
                ? ""
                : MESSAGES_BUNDLE.getString("messages.omitted_sources") + omittedSources.stream()
                    .map(source -> "\n\t- " + source)
                    .collect(Collectors.joining());
            String translationsWithNoSourcesText = notFoundTranslations.isEmpty()
                ? ""
                : (omittedSourcesText.isEmpty() ? "" : "\n")
                    + MESSAGES_BUNDLE.getString("messages.omitted_translations_with_unfound_sources") + notFoundTranslations.stream()
                        .map(translation -> "\n\t- " + translation)
                        .collect(Collectors.joining());
            String omittedFilesText = omittedSourcesText + translationsWithNoSourcesText;
            NotificationUtil.showWarningMessage(project, omittedFilesText);
        }
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
