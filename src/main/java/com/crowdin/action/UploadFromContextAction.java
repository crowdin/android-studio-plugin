package com.crowdin.action;

import com.crowdin.command.Crowdin;
import com.crowdin.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadFromContextAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        Crowdin crowdin = new Crowdin();
        Project project = anActionEvent.getProject();
        String branch = Utils.getCurrentBranch(project);
        crowdin.uploadFile(file, branch);
    }

    @Override
    public void update(AnActionEvent e) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean isSourceFile = isSourceFile(file);
        e.getPresentation().setEnabled(isSourceFile);
        e.getPresentation().setVisible(isSourceFile);
    }

    private static boolean isSourceFile(@Nullable VirtualFile file) {
        String sources = Utils.getPropertyValue("sources", true);
        List<String> sourcesList = Utils.getSourcesList(sources);
        if (file == null) {
            return false;
        }
        for (String src : sourcesList) {
            if (src.equals(file.getName()) && file.getParent() != null && "values".equals(file.getParent().getName())) {
                return true;
            }
        }
        return false;
    }
}
