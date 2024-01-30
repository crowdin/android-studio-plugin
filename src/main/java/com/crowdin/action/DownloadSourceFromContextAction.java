package com.crowdin.action;

import com.crowdin.logic.ContextLogic;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadSourceFromContextAction extends BackgroundAction {

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
                    false,
                    false,
                    true,
                    "messages.confirm.download_sources",
                    "Download"
            );

            if (context.isEmpty()) {
                return;
            }

            performDownload(this, file, context.get());
            NotificationUtil.showInformationMessage(project, MESSAGES_BUNDLE.getString("messages.success.download_source"));
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    public static void performDownload(Object requestor, VirtualFile file, ActionContext context) {
        Long sourceId = ContextLogic.findSourceIdFromSourceFile(context.properties, context.crowdinProjectCache.getFileInfos(context.branch), file, context.root);
        URL url = context.crowdin.downloadFile(sourceId);
        FileUtil.downloadFile(requestor, file, url);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean isSourceFile = false;
        try {
            Optional<ActionContext> context = super.prepare(project, null, false, false, false, null, null);

            if (context.isEmpty()) {
                return;
            }

            //hide for SB
            isSourceFile = !context.get().crowdinProjectCache.isStringsBased() && context.get().properties.getFiles()
                    .stream()
                    .flatMap(fb -> FileUtil.getSourceFilesRec(FileUtil.getProjectBaseDir(project), fb.getSource()).stream())
                    .anyMatch(f -> Objects.equals(file, f));
        } catch (Exception exception) {
//            do nothing
        } finally {
            e.getPresentation().setEnabled(isSourceFile);
            e.getPresentation().setVisible(isSourceFile);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.download_source_file_from_context"), (file != null ? file.getName() : "<UNKNOWN>"));
    }
}
