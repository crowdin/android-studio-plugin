package com.crowdin.ui.panel.progress.action;

import com.crowdin.action.BackgroundAction;
import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.translationstatus.model.FileBranchProgress;
import com.crowdin.client.translationstatus.model.LanguageProgress;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.util.ActionUtils;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;
import static com.crowdin.util.FileUtil.joinPaths;
import static com.crowdin.util.FileUtil.normalizePath;
import static com.crowdin.util.FileUtil.sepAtStart;
import static com.crowdin.util.FileUtil.unixPath;

public class RefreshTranslationProgressAction extends BackgroundAction {

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);

    public RefreshTranslationProgressAction() {
        super("Refresh data", "Refresh data", AllIcons.Actions.Refresh);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!isInProgress.get());
    }

    @Override
    protected void performInBackground(@NotNull AnActionEvent e, @NotNull ProgressIndicator indicator) {
        boolean forceRefresh = !CrowdinPanelWindowFactory.PLACE_ID.equals(e.getPlace());
        Project project = e.getProject();
        e.getPresentation().setEnabled(false);
        isInProgress.set(true);
        try {
            TranslationProgressWindow window = ServiceManager
                    .getService(project, CrowdinPanelWindowFactory.ProjectService.class)
                    .getTranslationProgressWindow();
            if (window == null) {
                return;
            }


            VirtualFile root = FileUtil.getProjectBaseDir(project);

            CrowdinProperties properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            NotificationUtil.setLogDebugLevel(properties.isDebug());
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

            String branchName = ActionUtils.getBranchName(project, properties, true);

            CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                    CrowdinProjectCacheProvider.getInstance(crowdin, branchName, forceRefresh);
            Branch branch = crowdinProjectCache.getBranches().get(branchName);

            Map<LanguageProgress, List<FileBranchProgress>> progress = crowdin.getProjectProgress()
                    .parallelStream()
                    .collect(Collectors.toMap(Function.identity(), langProgress -> crowdin.getLanguageProgress(langProgress.getLanguageId())));


            List<String> crowdinFilePaths = properties.getFiles().stream()
                    .flatMap((fileBean) -> {
                        List<VirtualFile> sourceFiles = FileUtil.getSourceFilesRec(root, fileBean.getSource());
                        return sourceFiles.stream().map(sourceFile -> {
                            if (properties.isPreserveHierarchy()) {
                                VirtualFile pathToPattern = FileUtil.getBaseDir(sourceFile, fileBean.getSource());

                                String relativePathToPattern = FileUtil.findRelativePath(FileUtil.getProjectBaseDir(project), pathToPattern);
                                String patternPathToFile = FileUtil.findRelativePath(pathToPattern, sourceFile.getParent());

                                return unixPath(sepAtStart(normalizePath(joinPaths(relativePathToPattern, patternPathToFile, sourceFile.getName()))));
                            } else {
                                return unixPath(sepAtStart(sourceFile.getName()));
                            }
                        });
                    })
                    .collect(Collectors.toList());


            Map<Long, String> fileNames = crowdinProjectCache.getFileInfos(branch).values()
                    .stream()
                    .filter((fileInfo) -> crowdinFilePaths.contains(removeBranchNameInPath(fileInfo.getPath(), branchName)))
                    .collect(Collectors.toMap(FileInfo::getId, file -> removeBranchNameInPath(file.getPath(), branchName)));
            Map<String, String> languageNames = crowdinProjectCache.getProjectLanguages()
                    .stream()
                    .collect(Collectors.toMap(Language::getId, Language::getName));

            ApplicationManager.getApplication().invokeAndWait(() -> {
                window.setData(crowdinProjectCache.getProject().getName(), progress, fileNames, languageNames);
                window.rebuildTree();
            });
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            if (project != null && forceRefresh) {
                NotificationUtil.logErrorMessage(project, ex);
                NotificationUtil.showErrorMessage(project, ex.getMessage());
            }
        } finally {
            e.getPresentation().setEnabled(true);
            isInProgress.set(false);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return "Refresh translation progress";
    }

    private String removeBranchNameInPath(String path, String branchName) {
        return (StringUtils.isNotEmpty(branchName)) ? path.replaceAll("^/" + branchName, "") : path;
    }
}
