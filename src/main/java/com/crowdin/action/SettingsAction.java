package com.crowdin.action;

import com.crowdin.settings.CrowdinSettingsConfigurable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class SettingsAction extends AnAction {

    public SettingsAction() {
        super("Settings", "Plugin settings", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }

        ShowSettingsUtil.getInstance().showSettingsDialog(project, CrowdinSettingsConfigurable.class);
    }

}
