package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.FileBean;
import com.crowdin.client.sourcefiles.model.FileInfo;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.crowdin.Constants.DOWNLOAD_TOOLBAR_ID;
import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadSourcesAction extends BackgroundAction {

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
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text_download_sources");
    }

    @Override
    protected void performInBackground(@NotNull AnActionEvent anActionEvent, @NotNull ProgressIndicator indicator) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }

        isInProgress.set(true);
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

            List<Path> selectedFiles = Optional
                    .ofNullable(project.getService(ProjectService.class).getDownloadWindow())
                    .map(DownloadWindow::getSelectedFiles)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(str -> Paths.get(context.get().root.getPath(), str))
                    .toList();

            if (!selectedFiles.isEmpty()) {
                for (Path file : selectedFiles) {
                    try {
                        VirtualFile virtualFile = FileUtil.findVFileByPath(file);
                        DownloadSourceFromContextAction.performDownload(this, virtualFile, context.get());
                        NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download_sources.file_downloaded"), file));
                    } catch (Exception e) {
                        NotificationUtil.logErrorMessage(project, e);
                        NotificationUtil.showWarningMessage(project, e.getMessage());
                    }
                }
                return;
            }

            Map<String, FileInfo> filePaths = context.get().crowdinProjectCache.getFileInfos(context.get().branch);

            AtomicBoolean isAnyFileDownloaded = new AtomicBoolean(false);

            for (FileBean fileBean : context.get().properties.getFiles()) {
                Predicate<String> sourcePredicate = FileUtil.filePathRegex(fileBean.getSource(), context.get().properties.isPreserveHierarchy());
                Map<String, VirtualFile> localSourceFiles = (context.get().properties.isPreserveHierarchy())
                        ? Collections.emptyMap()
                        : FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource()).stream()
                        .collect(Collectors.toMap(VirtualFile::getPath, Function.identity()));
                List<String> foundSources = filePaths.keySet().stream()
                        .map(FileUtil::unixPath)
                        .filter(sourcePredicate)
                        .map(FileUtil::normalizePath)
                        .sorted()
                        .toList();

                if (foundSources.isEmpty()) {
                    NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.no_sources_for_pattern"), fileBean.getSource()));
                    return;
                }

                for (String foundSourceFilePath : foundSources) {
                    if (context.get().properties.isPreserveHierarchy()) {
                        Long fileId = filePaths.get(foundSourceFilePath).getId();
                        this.downloadFile(context.get().crowdin, fileId, context.get().root, foundSourceFilePath);
                        isAnyFileDownloaded.set(true);
                    } else {
                        List<String> fittingSources = localSourceFiles.keySet().stream()
                                .filter(localSourceFilePath -> localSourceFilePath.endsWith(foundSourceFilePath))
                                .toList();

                        if (fittingSources.isEmpty()) {
                            NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.file_no_representative"), foundSourceFilePath));
                            continue;
                        } else if (fittingSources.size() > 1) {
                            NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.file_not_one_representative"), foundSourceFilePath));
                            continue;
                        }

                        Long fileId = filePaths.get(foundSourceFilePath).getId();
                        VirtualFile file = localSourceFiles.get(fittingSources.get(0));
                        this.downloadFile(context.get().crowdin, fileId, file);
                        isAnyFileDownloaded.set(true);
                    }
                    NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download_sources.file_downloaded"), foundSourceFilePath));
                }
            }

            if (isAnyFileDownloaded.get()) {
                NotificationUtil.showInformationMessage(project, MESSAGES_BUNDLE.getString("messages.success.download_sources"));
            } else {
                NotificationUtil.showWarningMessage(project, MESSAGES_BUNDLE.getString("messages.failure.download_sources"));
            }

        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.logErrorMessage(project, e);
            NotificationUtil.showErrorMessage(project, e.getMessage());
        } finally {
            isInProgress.set(false);
        }
    }

    private void downloadFile(Crowdin client, Long fileId, VirtualFile root, String filePath) {
        URL url = client.downloadFile(fileId);
        try (InputStream data = url.openStream()) {
            VirtualFile file = FileUtil.createIfNeededFilePath(this, root, filePath);
            FileUtil.downloadFile(this, file, data);
        } catch (IOException e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.download_file"), filePath, e.getMessage()), e);
        }
    }

    private void downloadFile(Crowdin client, Long fileId, VirtualFile file) {
        URL url = client.downloadFile(fileId);
        try (InputStream data = url.openStream()) {
            FileUtil.downloadFile(this, file, data);
        } catch (IOException e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.download_file"), file.getPath(), e.getMessage()), e);
        }
    }
}
