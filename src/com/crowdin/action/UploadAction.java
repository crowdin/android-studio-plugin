package com.crowdin.action;

import com.crowdin.command.Crowdin;
import com.crowdin.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sun.jersey.api.client.ClientResponse;
import org.jetbrains.annotations.NotNull;

/**
 * Created by ihor on 1/10/17.
 */
@SuppressWarnings("ALL")
public class UploadAction extends AnAction {
    public UploadAction() {
        super("UploadAction");
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = project.getBaseDir();
        VirtualFile source = Utils.getSourceFile(virtualFile);
        Crowdin crowdin = new Crowdin();
        String branch = Utils.getCurrentBranch(project);
        ClientResponse clientResponse = crowdin.uploadFile(source, branch);
    }
}