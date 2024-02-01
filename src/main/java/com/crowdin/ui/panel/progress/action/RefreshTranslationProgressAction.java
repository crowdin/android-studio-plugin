package com.crowdin.ui.panel.progress.action;

import com.crowdin.action.ActionContext;
import com.crowdin.action.BackgroundAction;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.translationstatus.model.FileBranchProgress;
import com.crowdin.client.translationstatus.model.LanguageProgress;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.StringUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.crowdin.Constants.PROGRESS_GROUP_FILES_BY_FILE_ACTION;
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
        if (project == null) {
            return;
        }

        e.getPresentation().setEnabled(false);
        isInProgress.set(true);
        try {

            TranslationProgressWindow window = project.getService(ProjectService.class).getTranslationProgressWindow();
            if (window == null) {
                return;
            }

            Optional<ActionContext> context = super.prepare(project, indicator, false, false, forceRefresh, null, null);

            if (context.isEmpty()) {
                return;
            }

            Map<LanguageProgress, List<FileBranchProgress>> progress = context.get().crowdin.getProjectProgress()
                    .parallelStream()
                    .collect(Collectors.toMap(Function.identity(), langProgress -> context.get().crowdin.getLanguageProgress(langProgress.getLanguageId())));

            List<String> crowdinFilePaths = context.get().properties.getFiles().stream()
                    .flatMap((fileBean) -> {
                        List<VirtualFile> sourceFiles = FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource());
                        return sourceFiles.stream().map(sourceFile -> {
                            if (context.get().properties.isPreserveHierarchy()) {
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


            Map<Long, String> fileNames = context.get().crowdinProjectCache.getFileInfos(context.get().branch).values()
                    .stream()
                    .filter((fileInfo) -> crowdinFilePaths.contains(removeBranchNameInPath(fileInfo.getPath(), context.get().branchName)))
                    .collect(Collectors.toMap(FileInfo::getId, file -> removeBranchNameInPath(file.getPath(), context.get().branchName)));
            Map<String, String> languageNames = context.get().crowdinProjectCache.getProjectLanguages()
                    .stream()
                    .collect(Collectors.toMap(Language::getId, Language::getName));

            ApplicationManager.getApplication().invokeAndWait(() -> {
                window.setData(context.get().crowdinProjectCache.getProject().getName(), progress, fileNames, languageNames);
                window.rebuildTree();
                CrowdinPanelWindowFactory.updateToolbar(
                        PROGRESS_GROUP_FILES_BY_FILE_ACTION,
                        null,
                        true,
                        !context.get().crowdinProjectCache.isStringsBased()
                );
            });
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            if (forceRefresh) {
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
        return (!StringUtils.isEmpty(branchName)) ? path.replaceAll("^/" + branchName, "") : path;
    }
}
