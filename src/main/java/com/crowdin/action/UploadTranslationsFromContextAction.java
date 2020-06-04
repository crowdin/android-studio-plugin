package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadTranslationsFromContextAction extends BackgroundAction {


    @Override
    public void performInBackground(AnActionEvent anActionEvent) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        Project project = anActionEvent.getProject();
        try {
            VirtualFile root = project.getBaseDir();
            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
            String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);
            Branch branch = crowdinProjectCache.getBranches().get(branchName);

            properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                List<VirtualFile> sources = FileUtil.getSourceFilesRec(root, sourcePattern);
                sources.forEach(s -> {
                    VirtualFile baseDir = FileUtil.getBaseDir(s, sourcePattern);
                    String sourcePath = s.getName();
                    File crowdinFile = crowdinProjectCache.getFiles().get(branch).get(sourcePath);
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(translationPattern, sourcePath);
                    for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang);
                        Path translationFile = Paths.get(baseDir.getPath(), builtPattern);
                        int compare = translationFile.compareTo(Paths.get(file.getPath()));
                        if (compare == 0) {
                            boolean uploaded = crowdin.uploadTranslationFile(translationFile.toFile(), crowdinFile.getId(), lang.getId());
                            if (uploaded) {
                                NotificationUtil.showInformationMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translation"), sourcePath));
                            }
                            return;
                        }
                    }
                });
            });


        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());

        CrowdinProperties properties;
        try {
            properties = CrowdinPropertiesLoader.load(project);
        } catch (Exception exception) {
            return;
        }
        VirtualFile root = project.getBaseDir();
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
        boolean isSourceFile = translations.contains(filePath);
        e.getPresentation().setEnabled(isSourceFile);
        e.getPresentation().setVisible(isSourceFile);
    }

    @Override
    String loadingText(AnActionEvent e) {
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources_from_context"), CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName());
    }
}
