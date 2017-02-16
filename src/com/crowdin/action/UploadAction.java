package com.crowdin.action;

import com.crowdin.command.Crowdin;
import com.crowdin.event.FileChangeListener;
import com.crowdin.utils.Utils;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import com.sun.jersey.api.client.ClientResponse;
import git4idea.GitLocalBranch;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.repo.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

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