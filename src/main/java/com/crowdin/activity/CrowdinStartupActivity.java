package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.event.FileChangeListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class CrowdinStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        new FileChangeListener(project);
        //config validation
        new Crowdin(project);
    }
}
