package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.client.config.CrowdinConfig;
import com.crowdin.client.config.CrowdinPropertiesLoader;
import com.crowdin.event.FileChangeListener;
import com.crowdin.logic.BranchLogic;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class CrowdinStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        try {
            new FileChangeListener(project);

            if (CrowdinPropertiesLoader.isWorkspaceNotPrepared(project)) {
                return;
            }

            //config validation
            CrowdinConfig properties = CrowdinPropertiesLoader.load(project);
            Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            this.reloadPlugin(project, crowdin, properties);
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }

    private void reloadPlugin(Project project, Crowdin crowdin, CrowdinConfig properties) {
        BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
        String branchName = branchLogic.acquireBranchName();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Crowdin") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Updating Crowdin cache");
                    project.getService(CrowdinProjectCacheProvider.class).getInstance(crowdin, branchName, true);
                    ProjectService service = project.getService(ProjectService.class);
                    EnumSet<ProjectService.InitializationItem> loadedComponents = service.addAndGetLoadedComponents(ProjectService.InitializationItem.STARTUP_ACTIVITY);
                    if (loadedComponents.contains(ProjectService.InitializationItem.UI_PANELS)) {
                        CrowdinPanelWindowFactory.reloadPanels(project, true);
                    }
                } catch (Exception e) {
                    NotificationUtil.showErrorMessage(project, e.getMessage());
                }
            }
        });
    }
}
