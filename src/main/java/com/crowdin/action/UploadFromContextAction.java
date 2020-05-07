package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.crowdin.util.FileUtil.PARENT_FOLDER_NAME;
import static com.crowdin.util.PropertyUtil.PROPERTY_SOURCES;

/**
 * Created by ihor on 1/27/17.
 */
public class UploadFromContextAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        Crowdin crowdin = new Crowdin();
        Project project = anActionEvent.getProject();
        String branch = GitUtil.getCurrentBranch(project);
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
        String sources = PropertyUtil.getPropertyValue(PROPERTY_SOURCES);
        List<String> sourcesList = FileUtil.getSourcesList(sources);
        if (file == null) {
            return false;
        }
        for (String src : sourcesList) {
            if (src.equals(file.getName()) && file.getParent() != null && PARENT_FOLDER_NAME.equals(file.getParent().getName())) {
                return true;
            }
        }
        return false;
    }
}
