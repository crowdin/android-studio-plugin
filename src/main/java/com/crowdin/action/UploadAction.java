package com.crowdin.action;

import com.crowdin.client.FileBean;
import com.crowdin.logic.SourceLogic;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/10/17.
 */
@SuppressWarnings("ALL")
public class UploadAction extends BackgroundAction {
    @Override
    public void performInBackground(@NotNull final AnActionEvent anActionEvent, ProgressIndicator indicator) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }

        try {
            Optional<ActionContext> context = this.prepare(
                    project,
                    indicator,
                    false,
                    true,
                    true,
                    "messages.confirm.upload_sources",
                    "Upload"
            );

            if (context.isEmpty()) {
                return;
            }

            List<String> selectedFiles = Optional
                    .ofNullable(project.getService(ProjectService.class).getUploadWindow())
                    .map(UploadWindow::getSelectedFiles)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(str -> Paths.get(context.get().root.getPath(), str).toString())
                    .toList();

            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.upload_sources.list_of_patterns")
                    + context.get().properties.getFiles().stream()
                    .map(fileBean -> String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.list_of_patterns_item"), fileBean.getSource(), fileBean.getTranslation()))
                    .collect(Collectors.joining()));

            Map<FileBean, List<VirtualFile>> sources = context.get().properties.getFiles()
                    .stream()
                    .map(fileBean -> new AbstractMap.SimpleEntry<>(
                                    fileBean,
                                    FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource())
                                            .stream()
                                            .filter(f -> selectedFiles.isEmpty() || selectedFiles.contains(Paths.get(f.getPath()).toString()))
                                            .toList()
                            )
                    )
                    .filter(e -> !e.getValue().isEmpty())
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            SourceLogic.processSources(project, context.get().root, context.get().crowdin, context.get().crowdinProjectCache, context.get().branch, context.get().properties.isPreserveHierarchy(), sources);

            project.getService(CrowdinProjectCacheProvider.class).outdateBranch(context.get().branchName);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources");
    }
}
