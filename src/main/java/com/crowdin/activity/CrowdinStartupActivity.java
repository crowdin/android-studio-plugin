package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.event.FileChangeListener;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class CrowdinStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        System.out.println("On");
        new FileChangeListener(project);
        System.out.println(project);
        if (PropertyUtil.getCrowdinPropertyFile(project) != null) {
            //config validation
            new Crowdin(project);
        }
    }
}
