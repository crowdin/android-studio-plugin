package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.event.FileChangeListener;
import com.crowdin.util.ActionUtils;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
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
            Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

            String branchName = ActionUtils.getBranchName(project, properties, false);

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Crowdin") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setText("Updating Crowdin cache");
                        CrowdinProjectCacheProvider.getInstance(crowdin, branchName, true);
                    } catch (Exception e) {
                        NotificationUtil.showErrorMessage(project, e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }
}
