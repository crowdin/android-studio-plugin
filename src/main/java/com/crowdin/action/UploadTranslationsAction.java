package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.util.*;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.crowdin.util.PropertyUtil.PROPERTY_SOURCES;

public class UploadTranslationsAction extends AnAction {

    private static final String PATTERN = "/values-%android_code%/%original_file_name%";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile root = project.getBaseDir();
        String sourcesProp = PropertyUtil.getPropertyValue(PROPERTY_SOURCES, project);
        List<String> sourcesList = FileUtil.getSourcesList(sourcesProp);
        Crowdin crowdin = new Crowdin(project);

        List<Language> projectLanguages = crowdin.getProjectLanguages();

        String branch = GitUtil.getCurrentBranch(project);
        Long branchId = crowdin.getBranch(branch).map(Branch::getId).orElse(null);

        List<com.crowdin.client.sourcefiles.model.File> files = crowdin.getFiles(branchId);
        Map<Long, Directory> dirs = crowdin.getDirectories(branchId);
        Map<String, File> filePaths = CrowdinFileUtil.buildFilePaths(files, dirs);

        int uploadedFilesCounter = 0;

        for (String src : sourcesList) {
            VirtualFile source = FileUtil.getSourceFile(root, src);
            String baseDir = source.getParent().getParent().getPath() + "/";
            String sourcePath = source.getName();

            File crowdinSource = filePaths.get(sourcePath);
            if (crowdinSource == null) {
                NotificationUtil.showWarningMessage(project, "File '" + (branch != null ? branch + "/" : "") + sourcePath + "' is missing in the project. Run 'Upload' to upload the missing source");
                continue;
            }
            String pattern1 = PlaceholderUtil.replaceFilePlaceholders(PATTERN, sourcePath);
            for (Language lang : projectLanguages) {
                String pattern2 = PlaceholderUtil.replaceLanguagePlaceholders(pattern1, lang);
                java.io.File translationFile = new java.io.File(baseDir + pattern2);
                if (!translationFile.exists()) {
                    continue;
                }
                crowdin.uploadTranslationFile(translationFile, crowdinSource.getId(), lang.getId());
                uploadedFilesCounter++;
            }
        }
        NotificationUtil.showInformationMessage(project, "Uploaded " + uploadedFilesCounter + " files");
    }
}
