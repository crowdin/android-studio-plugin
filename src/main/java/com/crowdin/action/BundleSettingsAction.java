package com.crowdin.action;

import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.download.DownloadWindow;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.crowdin.Constants.TOOLWINDOW_ID;

public class BundleSettingsAction extends AnAction {

    private boolean enabled = false;
    private boolean visible = false;
    private String text = "";

    public BundleSettingsAction() {
        super("Bundle Settings", "Bundle Settings", AllIcons.General.Settings);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (e.getPlace().equals(TOOLWINDOW_ID)) {
            this.enabled = e.getPresentation().isEnabled();
            this.visible = e.getPresentation().isVisible();
            this.text = e.getPresentation().getText();
        }
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(visible);
        e.getPresentation().setText(text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }

        DownloadWindow window = project.getService(ProjectService.class).getDownloadWindow();
        if (window == null || window.getSelectedBundle() == null) {
            return;
        }

        String link = window.buildBundleUrl();
        BrowserUtil.browse(link);
    }

}
