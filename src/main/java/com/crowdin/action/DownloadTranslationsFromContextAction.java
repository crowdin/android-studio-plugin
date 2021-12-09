package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.FileBean;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.logic.DownloadTranslationsLogic;
import com.crowdin.util.ActionUtils;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.crowdin.util.UIUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.NonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadTranslationsFromContextAction extends BackgroundAction {
    @Override
    protected void performInBackground(@NonNull AnActionEvent anActionEvent, @NonNull ProgressIndicator indicator) {
        Project project = anActionEvent.getProject();
        try {
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

            BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
            String branchName = branchLogic.acquireBranchName(true);
            indicator.checkCanceled();

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);

            if (!crowdinProjectCache.isManagerAccess()) {
                NotificationUtil.showErrorMessage(project, "You need to have manager access to perform this action");
                return;
            }

            Branch branch = branchLogic.getBranch(crowdinProjectCache, false);

            (new DownloadTranslationsLogic(project, crowdin, properties, root, crowdinProjectCache, branch)).process();
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
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

            String branchName = ActionUtils.getBranchName(project, properties, false);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, false);

            List<Path> translations = new ArrayList<>();
            for (FileBean fileBean : properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(root, fileBean.getSource())) {
                    VirtualFile baseDir = FileUtil.getBaseDir(source, fileBean.getSource());
                    String sourcePath = source.getName();
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(fileBean.getTranslation(), sourcePath);
                    for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang, crowdinProjectCache.getLanguageMapping());
                        Path translationFile = Paths.get(baseDir.getPath(), builtPattern);
                        translations.add(translationFile);
                    }
                }
            }
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
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.download");
    }
}
