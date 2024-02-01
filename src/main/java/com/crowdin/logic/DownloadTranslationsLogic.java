package com.crowdin.logic;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.FileBean;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.translations.model.BuildProjectTranslationRequest;
import com.crowdin.client.translations.model.ProjectBuild;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.util.CrowdinFileUtil;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.crowdin.util.StringUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadTranslationsLogic {

    private final Project project;
    private final Crowdin crowdin;
    private final CrowdinProperties properties;
    private final VirtualFile root;
    private final CrowdinProjectCacheProvider.CrowdinProjectCache projectCache;
    private final Branch branch;

    public DownloadTranslationsLogic(
            Project project, Crowdin crowdin, CrowdinProperties properties, VirtualFile root, CrowdinProjectCacheProvider.CrowdinProjectCache projectCache, Branch branch
    ) {
        this.project = project;
        this.crowdin = crowdin;
        this.properties = properties;
        this.root = root;
        this.projectCache = projectCache;
        this.branch = branch;
    }

    public void process() {
        File archive = null;
        String tempDir = null;
        try {
            archive = downloadArchive();
            tempDir = archive.getParent() + File.separator + "all" + System.nanoTime();

            NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.extract_files"), archive));

            FileUtil.extractArchive(archive, tempDir);

            List<File> files = FileUtil.walkDir(Paths.get(tempDir));

            List<Pair<File, File>> targets = findAllTranslations(tempDir, files);
            extractTranslations(targets);

            notifyAboutOmittedFiles(targets, files, tempDir);
        } finally {
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.download.clearing"));
            FileUtil.clear(root, archive, tempDir);
        }
    }

    public File downloadArchive() {
        NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.download.download_archive"));
        BuildProjectTranslationRequest request = RequestBuilder.buildProjectTranslationsRequest(branch != null ? branch.getId() : null);

        ProjectBuild projectBuild = crowdin.startBuildingTranslation(request);
        Long buildId = projectBuild.getId();

        while (!projectBuild.getStatus().equalsIgnoreCase("finished")) {
            projectBuild = crowdin.checkBuildingStatus(buildId);
        }

        URL url = crowdin.downloadProjectTranslations(buildId);

        try {
            return FileUtil.downloadTempFile(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't download file", e);
        }
    }

    public List<Pair<File, File>> findAllTranslations(String tempDir, List<java.io.File> files) {
        List<Pair<File, File>> targets = new ArrayList<>();
        for (FileBean fileBean : properties.getFiles()) {
            for (VirtualFile source : FileUtil.getSourceFilesRec(root, fileBean.getSource())) {
                VirtualFile pathToPattern = FileUtil.getBaseDir(source, fileBean.getSource());
                String sourceRelativePath = StringUtils.removeStart(source.getPath(), root.getPath());
                String relativePathToPattern = (properties.isPreserveHierarchy())
                        ? File.separator + FileUtil.findRelativePath(root, pathToPattern)
                        : File.separator;
                Map<Language, String> translationPaths =
                        PlaceholderUtil.buildTranslationPatterns(sourceRelativePath, fileBean.getTranslation(),
                                projectCache.getProjectLanguages(), projectCache.getLanguageMapping());
                for (Map.Entry<Language, String> translationPathEntry : translationPaths.entrySet()) {
                    File fromFile = new File(FileUtil.joinPaths(tempDir, relativePathToPattern, translationPathEntry.getValue()));
                    File toFile = new File(FileUtil.joinPaths(pathToPattern.getPath(), translationPathEntry.getValue()));
                    if (!files.contains(fromFile)) {
                        NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.file_not_found"),
                                FileUtil.joinPaths(relativePathToPattern, translationPathEntry.getValue())));
                        continue;
                    }
                    NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.file_found"),
                            FileUtil.joinPaths(relativePathToPattern, translationPathEntry.getValue())));
                    targets.add(Pair.create(fromFile, toFile));
                }
            }
        }
        return targets;
    }

    public void extractTranslations(List<Pair<File, File>> targets) {
        for (Pair<File, File> target : targets) {
            File fromFile = target.first;
            File toFile = target.second;
            toFile.getParentFile().mkdirs();
            if (!fromFile.renameTo(toFile) && toFile.delete() && !fromFile.renameTo(toFile)) {
                NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.extract_file"), toFile));
            }
        }
    }

    public void notifyAboutOmittedFiles(List<Pair<File, File>> targets, List<java.io.File> files, String tempDir) {
        Map<String, String> allCrowdinTranslationsWithSources = CrowdinFileUtil.buildAllProjectTranslationsWithSources(
                new ArrayList<>(projectCache.getFiles(branch).values()),
                CrowdinFileUtil.revDirPaths(projectCache.getDirs().getOrDefault(branch, new HashMap<>())),
                projectCache.getProjectLanguages(),
                projectCache.getLanguageMapping()
        );

        List<File> foundTranslations = targets.stream().map(p -> p.getFirst()).collect(Collectors.toList());
        List<File> omittedTranslations = files.stream()
                .filter(f -> !foundTranslations.contains(f))
                .collect(Collectors.toList());

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
    }
}
