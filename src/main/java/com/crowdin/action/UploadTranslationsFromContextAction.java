package com.crowdin.action;

import com.crowdin.client.*;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadTranslationsFromContextAction extends BackgroundAction {


    @Override
    public void performInBackground(AnActionEvent anActionEvent, ProgressIndicator indicator) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        Project project = anActionEvent.getProject();
        try {
            CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

            boolean confirmation = UIUtil.сonfirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString("messages.confirm.upload_translation_file"), "Upload");
            if (!confirmation) {
                return;
            }
            indicator.checkCanceled();

            VirtualFile root = FileUtil.getProjectBaseDir(project);
            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

            if (!CrowdinFileUtil.isValidBranchName(branchName)) {
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.branch_contains_forbidden_symbols"));
            }
            indicator.checkCanceled();

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);
            Branch branch = crowdinProjectCache.getBranches().get(branchName);


            Map<String, File> filePaths = crowdinProjectCache.getFiles().getOrDefault(branch, new HashMap<>());

            indicator.checkCanceled();
            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
                sources.forEach(s -> {

                    VirtualFile pathToPattern = FileUtil.getBaseDir(s, sourcePattern);

                    String relativePathToPattern = (properties.isPreserveHierarchy())
                        ? java.io.File.separator + FileUtil.findRelativePath(root, pathToPattern)
                        : "";
                    String patternPathToFile = (properties.isPreserveHierarchy())
                        ? java.io.File.separator + FileUtil.findRelativePath(pathToPattern, s.getParent())
                        : "";

                    File crowdinSource = filePaths.get(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, s.getName()));
                    if (crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_source"), (branchName != null ? branchName + "/" : "") + FileUtil.joinPaths(relativePathToPattern, patternPathToFile, s.getName())));
                        return;
                    }
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(translationPattern, FileUtil.joinPaths(relativePathToPattern, patternPathToFile, s.getName()));
                    for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang);
                        Path translationFile = Paths.get(pathToPattern.getPath(), builtPattern);
                        int compare = translationFile.compareTo(Paths.get(file.getPath()));
                        if (compare == 0) {
                            Long storageId;
                            try (InputStream translationFileStrem = new FileInputStream(translationFile.toFile())) {
                                storageId = crowdin.addStorage(translationFile.toFile().getName(), translationFileStrem);
                            } catch (IOException exception) {
                                throw new RuntimeException("Unhandled exception with File '" + translationFile + "'", exception);
                            }
                            UploadTranslationsRequest request = RequestBuilder.uploadTranslation(crowdinSource.getId(), storageId);
                            try {
                                crowdin.uploadTranslation(lang.getId(), request);
                                NotificationUtil.showInformationMessage(project,
                                    String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translation"),
                                        FileUtil.noSepAtStart(FileUtil.joinPaths(relativePathToPattern, builtPattern))));
                            } catch (Exception exception) {
                                NotificationUtil.showErrorMessage(project, "Couldn't upload translation file '" + translationFile + "': " + exception.getMessage());
                            }
                        }
                    }
                });
            });
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean isTranslationFile = false;
        try {
            CrowdinProperties properties;
            try {
                properties = CrowdinPropertiesLoader.load(project);
            } catch (Exception exception) {
                return;
            }
            NotificationUtil.setLogDebugLevel(properties.isDebug());
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

            VirtualFile root = FileUtil.getProjectBaseDir(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, false);

            List<Path> translations = new ArrayList<>();
            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
                sources.forEach(s -> {
                    VirtualFile baseDir = FileUtil.getBaseDir(s, sourcePattern);
                    String sourcePath = s.getName();
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(translationPattern, sourcePath);
                    for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang);
                        Path translationFile = Paths.get(baseDir.getPath(), builtPattern);
                        translations.add(translationFile);
                    }
                });
            });
            Path filePath = Paths.get(file.getPath());
            isTranslationFile = translations.contains(filePath);
        } catch (Exception exception) {
//            do nothing
        } finally {
            e.getPresentation().setEnabled(isTranslationFile);
            e.getPresentation().setVisible(isTranslationFile);
        }
    }

    @Override
    String loadingText(AnActionEvent e) {
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources_from_context"), CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName());
    }
}
