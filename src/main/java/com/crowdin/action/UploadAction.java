package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.crowdin.util.PropertyUtil.PROPERTY_SOURCES;

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
        String sourcesProp = PropertyUtil.getPropertyValue(PROPERTY_SOURCES);
        List<String> sourcesList = FileUtil.getSourcesList(sourcesProp);
        Crowdin crowdin = new Crowdin();
        for (String src : sourcesList) {
            VirtualFile source = FileUtil.getSourceFile(virtualFile, src);
            String branch = GitUtil.getCurrentBranch(project);
            crowdin.uploadFile(source, branch);
        }
    }
}
