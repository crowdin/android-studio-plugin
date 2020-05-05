package com.crowdin.action;

import com.crowdin.command.Crowdin;
import com.crowdin.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

/**
 * Created by ihor on 1/24/17.
 */
public class DownloadAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = project.getBaseDir();
        VirtualFile source = Utils.getSourceFile(virtualFile, null);
        Crowdin crowdin = new Crowdin();
        String branch = Utils.getCurrentBranch(project);
        File downloadTranslations = crowdin.downloadTranslations(source, branch);
        Utils.extractTranslations(downloadTranslations);
        if (downloadTranslations.delete()) {
            System.out.println("all.zip was deleted");
        } else {
            System.out.println("all.zip wasn't deleted");
        }
    }
}
