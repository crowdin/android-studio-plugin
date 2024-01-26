package com.crowdin.action;

import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.FileBean;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadFromContextAction extends BackgroundAction {
    @Override
    public void performInBackground(AnActionEvent anActionEvent, @NotNull ProgressIndicator indicator) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
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
                    "messages.confirm.upload_source_file",
                    "Upload"
            );

            if (context.isEmpty()) {
                return;
            }

            FileBean foundFileBean = context.get().properties.getFiles()
                    .stream()
                    .filter(fb -> FileUtil.getSourceFilesRec(context.get().root, fb.getSource()).contains(file))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Unexpected error: couldn't find suitable source pattern"));

            indicator.checkCanceled();

            Map<FileBean, List<VirtualFile>> source = Collections.singletonMap(foundFileBean, Collections.singletonList(file));
            SourceLogic.processSources(project, context.get().root, context.get().crowdin, context.get().crowdinProjectCache, context.get().branch, context.get().properties.isPreserveHierarchy(), source);

            CrowdinProjectCacheProvider.outdateBranch(context.get().branchName);
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
        boolean isSourceFile = false;
        try {
            CrowdinProperties properties;
            properties = CrowdinPropertiesLoader.load(project);
            isSourceFile = properties.getFiles()
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
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources_from_context"), CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName());
    }
}
