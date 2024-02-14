package com.crowdin.action;

import com.crowdin.client.RequestBuilder;
import com.crowdin.client.config.CrowdinPropertiesLoader;
import com.crowdin.client.languages.model.Language;
import com.crowdin.logic.ContextLogic;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadTranslationFromContextAction extends BackgroundAction {

    @Override
    protected void performInBackground(@NotNull AnActionEvent anActionEvent, @NotNull ProgressIndicator indicator) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());

        if (file == null) {
            return;
        }

        Project project = anActionEvent.getProject();

        if (project == null) {
            return;
        }

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

            performDownload(this, context.get(), file);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        } finally {
            ApplicationManager.getApplication().invokeAndWait(() -> CrowdinPanelWindowFactory.reloadPanels(project, false));
        }
    }

    public static void performDownload(Object requestor, ActionContext context, VirtualFile file) {
        Map.Entry<VirtualFile, Language> source = ContextLogic.findSourceFileFromTranslationFile(file, context.properties, context.root, context.crowdinProjectCache)
                .orElseThrow(() -> new RuntimeException(MESSAGES_BUNDLE.getString("errors.file_no_representative_context")));

        Long sourceId = ContextLogic.findSourceIdFromSourceFile(context.properties, context.crowdinProjectCache.getFileInfos(context.branch), source.getKey(), context.root);

        URL url = context.crowdin.downloadFileTranslation(sourceId, RequestBuilder.buildProjectFileTranslation(source.getValue().getId()));
        FileUtil.downloadFile(requestor, file, url);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean isTranslationFile = false;

        try {
            if (file == null) {
                return;
            }

            if (CrowdinPropertiesLoader.isWorkspaceNotPrepared(project)) {
                return;
            }

            Optional<ActionContext> context = super.prepare(project, null, false, false, false, null, null);

            if (context.isEmpty()) {
                return;
            }

            //hide for SB
            isTranslationFile = !context.get().crowdinProjectCache.isStringsBased() && ContextLogic.findSourceFileFromTranslationFile(file, context.get().properties, context.get().root, context.get().crowdinProjectCache).isPresent();
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
