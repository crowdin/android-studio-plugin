package com.crowdin.action;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.logic.DownloadBundleLogic;
import com.crowdin.logic.DownloadTranslationsLogic;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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
            Optional<ActionContext> contextOptional = super.prepare(
                    project,
                    indicator,
                    true,
                    false,
                    true,
                    "messages.confirm.download",
                    "Download"
            );

            if (contextOptional.isEmpty()) {
                return;
            }

            if (contextOptional.get().crowdinProjectCache.isStringsBased()) {
                if (contextOptional.get().branch == null) {
                    NotificationUtil.showErrorMessage(project, "Branch is missing");
                    return;
                }

                DownloadWindow window = ServiceManager
                        .getService(project, CrowdinPanelWindowFactory.ProjectService.class)
                        .getDownloadWindow();
                if (window == null) {
                    return;
                }

                Bundle bundle = window.getSelectedBundle();

                if (bundle == null) {
                    NotificationUtil.showErrorMessage(project, "Bundle not selected");
                    return;
                }

                (new DownloadBundleLogic(project, contextOptional.get().crowdin, contextOptional.get().root, bundle)).process();
                return;
            }


            (new DownloadTranslationsLogic(project, contextOptional.get().crowdin, contextOptional.get().properties, contextOptional.get().root, contextOptional.get().crowdinProjectCache, contextOptional.get().branch)).process();
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
