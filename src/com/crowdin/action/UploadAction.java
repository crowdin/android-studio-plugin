package com.crowdin.action;

import com.crowdin.command.Crowdin;
import com.crowdin.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by ihor on 1/10/17.
 */
@SuppressWarnings("ALL")
public class UploadAction extends AnAction {
    public UploadAction() {
        super("UploadAction");
    }

    public static final String PROPERTY_SOURCES = "sources";

    @Override
    public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = project.getBaseDir();
        String sourcesProp = Utils.getPropertyValue(PROPERTY_SOURCES, true);
        List<String> sourcesList = Utils.getSourcesList(sourcesProp);
        Crowdin crowdin = new Crowdin();
        for (String src : sourcesList) {
            VirtualFile source = Utils.getSourceFile(virtualFile, src);
            String branch = Utils.getCurrentBranch(project);
            crowdin.uploadFile(source, branch);
        }
    }
}