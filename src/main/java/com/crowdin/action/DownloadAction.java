package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DownloadAction extends AnAction {

    private static final Pattern TRANSLATION_PATTERN = Pattern.compile("[\\\\\\\\\\/]?values-([a-zA-Z-]+)[\\\\\\\\\\/]([^\\\\\\\\|^\\/]+)$");

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = project.getBaseDir();
        VirtualFile source = FileUtil.getSourceFile(virtualFile, null);
        Crowdin crowdin = new Crowdin(project);
        String branch = GitUtil.getCurrentBranch(project);
        File downloadTranslations = crowdin.downloadTranslations(source, branch);
        if (downloadTranslations != null) {
            String tempDir = downloadTranslations.getParent() + File.separator + "all" + System.nanoTime() + File.separator;
            this.extractTranslations(downloadTranslations, tempDir);
            List<String> files = FileUtil.walkDir(Paths.get(tempDir)).stream()
                    .map(File::getAbsolutePath)
                    .map(path -> StringUtils.removeStart(path, tempDir))
                    .collect(Collectors.toList());

            List<String> androidCodes = crowdin.getSupportedLanguageAndroidCodes();
            List<String> sortedFiles = filterFiles(files, androidCodes);
            sortedFiles.forEach(filePath -> {
                File fromFile = new File(tempDir + filePath);
                File toFile = new File(downloadTranslations.getParent() + File.separator + filePath);
                toFile.getParentFile().mkdirs();
                if (!fromFile.renameTo(toFile) && toFile.delete() && !fromFile.renameTo(toFile)) {
                    NotificationUtil.showWarningMessage("Failed to extract file '" + toFile + "'.");
                }
            });
            if (downloadTranslations.delete()) {
                System.out.println("all.zip was deleted");
            } else {
                System.out.println("all.zip wasn't deleted");
            }
            try {
                FileUtils.deleteDirectory(new File(tempDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
            virtualFile.refresh(true, true);
            NotificationUtil.showInformationMessage("Translations successfully downloaded");
        }
    }

    private void extractTranslations(File archive, String dirPath) {
        if (archive == null) {
            return;
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(archive);
        } catch (ZipException e) {
            e.printStackTrace();
        }

        try {
            zipFile.extractAll(dirPath);
        } catch (ZipException e) {
            NotificationUtil.showInformationMessage("Downloading translations failed");
            e.printStackTrace();
        }
    }

    private List<String> filterFiles(List<String> files, List<String> androidCodes) {
        return files.stream()
            .filter(file -> {
                Matcher matcher = TRANSLATION_PATTERN.matcher(file);
                return (matcher.matches() && androidCodes.contains(matcher.group(1)));
            })
            .collect(Collectors.toList());
    }
}
