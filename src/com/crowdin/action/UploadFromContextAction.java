package com.crowdin.action;

import com.crowdin.command.Crowdin;
import com.crowdin.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sun.jersey.api.client.ClientResponse;
import org.jetbrains.annotations.Nullable;

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
        ClientResponse clientResponse = crowdin.uploadFile(file, branch);
    }

    @Override
    public void update(AnActionEvent e) {

        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());

        boolean isStringXML = isStringXml(file);
        e.getPresentation().setEnabled(isStringXML);
        e.getPresentation().setVisible(isStringXML);
    }

    private static boolean isStringXml(@Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }
        if (!"strings.xml".equals(file.getName())) {
            return false;
        }
        if (file.getParent() == null) {
            return false;
        }
        if (!"values".equals(file.getParent().getName())) {
            return false;
        }
        return true;
    }
}
