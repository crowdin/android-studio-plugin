package com.crowdin.ui.panel.download.action;

import com.crowdin.action.ActionContext;
import com.crowdin.action.BackgroundAction;
import com.crowdin.client.config.FileBean;
import com.crowdin.client.languages.model.Language;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.crowdin.util.StringUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends BackgroundAction {

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);

    public RefreshAction() {
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
            DownloadWindow window = project.getService(ProjectService.class).getDownloadWindow();
            if (window == null) {
                return;
            }

            Optional<ActionContext> context = super.prepare(project, indicator, false, false, forceRefresh, null, null);

            if (context.isEmpty()) {
                return;
            }

            if (context.get().crowdinProjectCache.isStringsBased()) {
                String url = context.get().crowdin.getBundlesUrl(context.get().crowdinProjectCache.getProject());
                ApplicationManager.getApplication()
                        .invokeAndWait(() -> window.rebuildBundlesTree(context.get().crowdinProjectCache.getProject().getName(), context.get().crowdinProjectCache.getBundles(), url));
                return;
            }

            List<String> files = new ArrayList<>();

            for (FileBean fileBean : context.get().properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource())) {
                    VirtualFile pathToPattern = FileUtil.getBaseDir(source, fileBean.getSource());
                    String sourceRelativePath = context.get().properties.isPreserveHierarchy() ? StringUtils.removeStart(source.getPath(), context.get().root.getPath()) : FileUtil.sepAtStart(source.getName());

                    Map<Language, String> translationPaths =
                            PlaceholderUtil.buildTranslationPatterns(sourceRelativePath, fileBean.getTranslation(), context.get().crowdinProjectCache.getProjectLanguages(), context.get().crowdinProjectCache.getLanguageMapping());

                    for (Map.Entry<Language, String> translationPath : translationPaths.entrySet()) {
                        java.io.File translationFile = Paths.get(pathToPattern.getPath(), translationPath.getValue()).toFile();
                        if (translationFile.exists()) {
                            String file = Paths.get(context.get().root.getPath()).relativize(Paths.get(translationFile.getPath())).toString();
                            files.add(file);
                        }
                    }
                }
            }

            ApplicationManager.getApplication()
                    .invokeAndWait(() -> window.rebuildFileTree(context.get().crowdinProjectCache.getProject().getName(), files));
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
        return "Refresh download panel";
    }

}
