package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.event.FileChangeListener;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.util.ActionUtils;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import org.jetbrains.annotations.NotNull;

public class CrowdinStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        try {
            new FileChangeListener(project);
            CrowdinProperties properties;
            if (PropertyUtil.getCrowdinPropertyFile(project) == null) {
                return;
            }
            //config validation
            properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            if (properties.isDisabledBranches()) {
                this.reloadPlugin(project, crowdin, properties);
                return;
            }

            //if branch is required then we need to wait when VCS will be fully initialized, otherwise GitBranchUtil will give us `null`
            project
                    .getMessageBus()
                    .connect()
                    .subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, () -> this.reloadPlugin(project, crowdin, properties));
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    private void reloadPlugin(Project project, Crowdin crowdin, CrowdinProperties properties) {
        String branchName = ActionUtils.getBranchName(project, properties, false);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Crowdin") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Updating Crowdin cache");
                    CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);
                    CrowdinPanelWindowFactory.reloadPanels(project, true);
                } catch (Exception e) {
                    NotificationUtil.showErrorMessage(project, e.getMessage());
                }
            }
        });
    }
}
