package com.crowdin.action;

import com.crowdin.client.FileBean;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.client.translations.model.UploadTranslationsStringsRequest;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.crowdin.util.StringUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.crowdin.Constants.MESSAGES_BUNDLE;
import static com.crowdin.util.Util.isFileFormatNotAllowed;

public class UploadTranslationsAction extends BackgroundAction {

    @Override
    public void performInBackground(@NotNull AnActionEvent e, @NotNull ProgressIndicator indicator) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        try {
            Optional<ActionContext> context = this.prepare(
                    project,
                    indicator,
                    true,
                    true,
                    true,
                    "messages.confirm.upload_translations",
                    "Upload"
            );

            if (context.isEmpty()) {
                return;
            }

            if (context.get().crowdinProjectCache.isStringsBased() && context.get().branch == null) {
                NotificationUtil.showErrorMessage(project, "Branch is missing");
                return;
            }

            Map<String, FileInfo> filePaths = context.get().crowdinProjectCache.isStringsBased()
                    ? Collections.emptyMap()
                    : context.get().crowdinProjectCache.getFileInfos(context.get().branch);

            if (!context.get().crowdinProjectCache.isStringsBased()) {
                NotificationUtil.logDebugMessage(project, "Project files: " + filePaths.keySet());
            }

            AtomicInteger uploadedFilesCounter = new AtomicInteger(0);

            for (FileBean fileBean : context.get().properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource())) {
                    VirtualFile pathToPattern = FileUtil.getBaseDir(source, fileBean.getSource());
                    String sourceRelativePath = context.get().properties.isPreserveHierarchy() ? StringUtils.removeStart(source.getPath(), context.get().root.getPath()) : FileUtil.sepAtStart(source.getName());

                    Map<Language, String> translationPaths =
                            PlaceholderUtil.buildTranslationPatterns(sourceRelativePath, fileBean.getTranslation(), context.get().crowdinProjectCache.getProjectLanguages(), context.get().crowdinProjectCache.getLanguageMapping());

                    FileInfo crowdinSource = filePaths.get(FileUtil.normalizePath(sourceRelativePath));
                    if (!context.get().crowdinProjectCache.isStringsBased() && crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_source"), FileUtil.normalizePath((context.get().branchName != null ? context.get().branchName + "/" : "") + sourceRelativePath)));
                        return;
                    }
                    for (Map.Entry<Language, String> translationPath : translationPaths.entrySet()) {
                        java.io.File translationFile = Paths.get(pathToPattern.getPath(), translationPath.getValue()).toFile();
                        if (!translationFile.exists()) {
                            NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_translation"), FileUtil.noSepAtStart(StringUtils.removeStart(translationFile.getPath(), context.get().root.getPath()))));
                            continue;
                        }

                        Long storageId;
                        try (InputStream translationFileStrem = new FileInputStream(translationFile)) {
                            storageId = context.get().crowdin.addStorage(translationFile.getName(), translationFileStrem);
                        } catch (IOException exception) {
                            throw new RuntimeException("Unhandled exception with File '" + translationFile + "'", exception);
                        }

                        try {

                            if (context.get().crowdinProjectCache.isStringsBased()) {
                                UploadTranslationsStringsRequest request = RequestBuilder.uploadStringsTranslation(context.get().branch.getId(), storageId, context.get().properties.isImportEqSuggestions(), context.get().properties.isAutoApproveImported(), context.get().properties.isTranslateHidden());
                                context.get().crowdin.uploadStringsTranslation(translationPath.getKey().getId(), request);
                            } else {
                                UploadTranslationsRequest request = RequestBuilder.uploadTranslation(crowdinSource.getId(), storageId, context.get().properties.isImportEqSuggestions(), context.get().properties.isAutoApproveImported(), context.get().properties.isTranslateHidden());
                                context.get().crowdin.uploadTranslation(translationPath.getKey().getId(), request);
                            }

                            uploadedFilesCounter.incrementAndGet();
                        } catch (Exception exception) {
                            if (isFileFormatNotAllowed(exception)) {
                                String message = String.format(
                                        "*.%s files are not allowed to upload in strings-based projects",
                                        source.getExtension()
                                );
                                NotificationUtil.showWarningMessage(project, message);
                            } else {
                                NotificationUtil.logErrorMessage(project, exception);
                                NotificationUtil.showErrorMessage(project, "Couldn't upload translation file '" + translationFile + "': " + exception.getMessage());
                            }
                        }
                    }
                }
            }

            if (uploadedFilesCounter.get() > 0) {
                NotificationUtil.showInformationMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translations"), uploadedFilesCounter.get()));
            } else {
                NotificationUtil.showWarningMessage(project, MESSAGES_BUNDLE.getString("errors.uploaded_zero_translations"));
            }
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (Exception exception) {
            NotificationUtil.logErrorMessage(project, exception);
            NotificationUtil.showErrorMessage(project, exception.getMessage());
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return MESSAGES_BUNDLE.getString("labels.loading_text.upload_translations");
    }
}
