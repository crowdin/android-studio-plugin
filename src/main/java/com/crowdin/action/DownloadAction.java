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

import java.io.File;

public class DownloadAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = project.getBaseDir();
        VirtualFile source = FileUtil.getSourceFile(virtualFile, null);
        Crowdin crowdin = new Crowdin();
        String branch = GitUtil.getCurrentBranch(project);
        File downloadTranslations = crowdin.downloadTranslations(source, branch);
        this.extractTranslations(downloadTranslations);
        if (downloadTranslations.delete()) {
            System.out.println("all.zip was deleted");
        } else {
            System.out.println("all.zip wasn't deleted");
        }
    }

    private void extractTranslations(File archive) {
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
            zipFile.extractAll(archive.getParent());
            NotificationUtil.showInformationMessage("Translations successfully downloaded");
        } catch (ZipException e) {
            NotificationUtil.showInformationMessage("Downloading translations failed");
            e.printStackTrace();
        }
    }
}
