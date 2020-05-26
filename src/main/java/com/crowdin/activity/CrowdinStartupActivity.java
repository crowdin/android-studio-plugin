package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.event.FileChangeListener;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class CrowdinStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        try {
            new FileChangeListener(project);
            if (PropertyUtil.getCrowdinPropertyFile(project) != null) {
                //config validation
                CrowdinPropertiesLoader.load(project);
            }
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
        }
    }
}
