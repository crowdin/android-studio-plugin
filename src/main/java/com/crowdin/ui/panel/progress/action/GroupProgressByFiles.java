package com.crowdin.ui.panel.progress.action;

import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class GroupProgressByFiles extends ToggleAction implements DumbAware {

    public GroupProgressByFiles() {
        super("Group by files", "Really group by files", AllIcons.Actions.GroupByFile);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        TranslationProgressWindow window = getTranslationProgressWindowOrNull(e.getProject());
        if (window == null) {
            return false;
        } else {
            return window.isGroupByFiles();
        }
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        TranslationProgressWindow window = getTranslationProgressWindowOrNull(e.getProject());
        if (window != null) {
            window.setGroupByFiles(state);
            window.rebuildTree();
        }
    }

    private TranslationProgressWindow getTranslationProgressWindowOrNull(Project project) {
        if (project == null) {
            return null;
        }

        return project.getService(ProjectService.class).getTranslationProgressWindow();
    }
}
