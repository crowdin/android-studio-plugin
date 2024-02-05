package com.crowdin.action;

import com.crowdin.client.RequestBuilder;
import com.crowdin.client.config.CrowdinPropertiesLoader;
import com.crowdin.client.config.FileBean;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.client.translations.model.UploadTranslationsStringsRequest;
import com.crowdin.logic.ContextLogic;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PlaceholderUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.crowdin.Constants.MESSAGES_BUNDLE;
import static com.crowdin.util.Util.isFileFormatNotAllowed;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadTranslationsFromContextAction extends BackgroundAction {

    @Override
    public void performInBackground(AnActionEvent anActionEvent, @NotNull ProgressIndicator indicator) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        if (file == null) {
            return;
        }

        Project project = anActionEvent.getProject();
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
                    "messages.confirm.upload_translation_file",
                    "Upload"
            );

            if (context.isEmpty()) {
                return;
            }

            if (context.get().crowdinProjectCache.isStringsBased() && context.get().branch == null) {
                NotificationUtil.showErrorMessage(project, "Branch is missing");
                return;
            }

            Map<String, File> filePaths = context.get().crowdinProjectCache.isStringsBased()
                    ? Collections.emptyMap()
                    : context.get().crowdinProjectCache.getFiles(context.get().branch);

            indicator.checkCanceled();
            for (FileBean fileBean : context.get().properties.getFiles()) {
                for (VirtualFile source : FileUtil.getSourceFilesRec(context.get().root, fileBean.getSource())) {
                    VirtualFile pathToPattern = FileUtil.getBaseDir(source, fileBean.getSource());

                    String relativePathToPattern = (context.get().properties.isPreserveHierarchy())
                            ? java.io.File.separator + FileUtil.findRelativePath(context.get().root, pathToPattern)
                            : "";
                    String patternPathToFile = (context.get().properties.isPreserveHierarchy())
                            ? java.io.File.separator + FileUtil.findRelativePath(pathToPattern, source.getParent())
                            : "";

                    File crowdinSource = filePaths.get(FileUtil.normalizePath(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName())));
                    if (!context.get().crowdinProjectCache.isStringsBased() && crowdinSource == null) {
                        NotificationUtil.showWarningMessage(project, String.format(MESSAGES_BUNDLE.getString("errors.missing_source"), (context.get().branchName != null ? context.get().branchName : "") + FileUtil.sepAtStart(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName()))));
                        return;
                    }
                    String basePattern = PlaceholderUtil.replaceFilePlaceholders(fileBean.getTranslation(), FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName()));
                    for (Language lang : context.get().crowdinProjectCache.getProjectLanguages()) {
                        String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang, context.get().crowdinProjectCache.getLanguageMapping());
                        Path translationFile = Paths.get(pathToPattern.getPath(), builtPattern);
                        int compare = translationFile.compareTo(Paths.get(file.getPath()));
                        if (compare == 0) {
                            Long storageId;
                            try (InputStream translationFileStream = new FileInputStream(translationFile.toFile())) {
                                storageId = context.get().crowdin.addStorage(translationFile.toFile().getName(), translationFileStream);
                            } catch (IOException exception) {
                                throw new RuntimeException("Unhandled exception with File '" + translationFile + "'", exception);
                            }

                            try {

                                if (context.get().crowdinProjectCache.isStringsBased()) {
                                    UploadTranslationsStringsRequest request = RequestBuilder.uploadStringsTranslation(context.get().branch.getId(), storageId, context.get().properties.isImportEqSuggestions(), context.get().properties.isAutoApproveImported(), context.get().properties.isTranslateHidden());
                                    context.get().crowdin.uploadStringsTranslation(lang.getId(), request);
                                } else {
                                    UploadTranslationsRequest request = RequestBuilder.uploadTranslation(crowdinSource.getId(), storageId, context.get().properties.isImportEqSuggestions(), context.get().properties.isAutoApproveImported(), context.get().properties.isTranslateHidden());
                                    context.get().crowdin.uploadTranslation(lang.getId(), request);
                                }

                                NotificationUtil.showInformationMessage(project,
                                        String.format(MESSAGES_BUNDLE.getString("messages.success.upload_translation"),
                                                FileUtil.noSepAtStart(FileUtil.joinPaths(relativePathToPattern, builtPattern))));
                            } catch (Exception exception) {
                                if (isFileFormatNotAllowed(exception)) {
                                    String message = String.format(
                                            "*.%s files are not allowed to upload in strings-based projects",
                                            source.getExtension()
                                    );
                                    NotificationUtil.showWarningMessage(project, message);
                                } else {
                                    NotificationUtil.showErrorMessage(project, "Couldn't upload translation file '" + translationFile + "': " + exception.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        } finally {
            ApplicationManager.getApplication().invokeAndWait(() -> CrowdinPanelWindowFactory.reloadPanels(project, true));
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

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

            isTranslationFile = ContextLogic.findSourceFileFromTranslationFile(file, context.get().properties, context.get().root, context.get().crowdinProjectCache).isPresent();
        } catch (Exception exception) {
//            do nothing
        } finally {
            e.getPresentation().setEnabled(isTranslationFile);
            e.getPresentation().setVisible(isTranslationFile);
        }
    }

    @Override
    protected String loadingText(AnActionEvent e) {
        return String.format(MESSAGES_BUNDLE.getString("labels.loading_text.upload_sources_from_context"), CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName());
    }
}
