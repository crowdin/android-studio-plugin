package com.crowdin.action;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.logic.DownloadBundleLogic;
import com.crowdin.logic.DownloadTranslationsLogic;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.crowdin.Constants.DOWNLOAD_TOOLBAR_ID;
import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadAction extends BackgroundAction {

    private boolean enabled = false;
    private boolean visible = false;
    private String text = "";

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (e.getPlace().equals(DOWNLOAD_TOOLBAR_ID)) {
            this.enabled = e.getPresentation().isEnabled();
            this.visible = e.getPresentation().isVisible();
            this.text = e.getPresentation().getText();
        }
        e.getPresentation().setEnabled(!isInProgress.get() && enabled);
        e.getPresentation().setVisible(visible);
        e.getPresentation().setText(text);
    }

    @Override
    public void performInBackground(AnActionEvent anActionEvent, @NotNull ProgressIndicator indicator) {
        Project project = anActionEvent.getProject();

        if (project == null) {
            return;
        }

        isInProgress.set(true);
        try {
            Optional<ActionContext> context = super.prepare(
                    project,
                    indicator,
                    true,
                    false,
                    true,
                    "messages.confirm.download",
                    "Download"
            );

            if (context.isEmpty()) {
                return;
            }

            if (context.get().crowdinProjectCache.isStringsBased()) {
                if (context.get().branch == null) {
                    NotificationUtil.showErrorMessage(project, "Branch is missing");
                    return;
                }

                DownloadWindow window = project.getService(ProjectService.class).getDownloadWindow();
                if (window == null) {
                    return;
                }

                Bundle bundle = window.getSelectedBundle();

                if (bundle == null) {
                    NotificationUtil.showErrorMessage(project, "Bundle not selected");
                    return;
                }

                (new DownloadBundleLogic(project, context.get().crowdin, context.get().root, bundle)).process();
                return;
            }

            List<String> selectedFiles = Optional
                    .ofNullable(project.getService(ProjectService.class).getDownloadWindow())
                    .map(DownloadWindow::getSelectedFiles)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(str -> Paths.get(context.get().root.getPath(), str).toString())
                    .toList();

            if (!selectedFiles.isEmpty()) {
                for (String file : selectedFiles) {
                    try {
                        VirtualFile virtualFile = FileUtil.findVFileByPath(file);
                        DownloadTranslationFromContextAction.performDownload(this, context.get(), virtualFile);
                    } catch (Exception e) {
                        NotificationUtil.logErrorMessage(project, e);
                        NotificationUtil.showWarningMessage(project, e.getMessage());
                    }
                }
                return;
            }


            (new DownloadTranslationsLogic(project, context.get().crowdin, context.get().properties, context.get().root, context.get().crowdinProjectCache, context.get().branch)).process();
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        } finally {
            isInProgress.set(false);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.download");
    }
}
