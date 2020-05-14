package com.crowdin.activity;

import com.crowdin.client.Crowdin;
import com.crowdin.event.FileChangeListener;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ihor on 1/26/17.
 */
public class CrowdinStartupActivity implements StartupActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrowdinStartupActivity.class);

    public static final String PROPERTY_AUTO_UPLOAD = "auto-upload";

    @Override
    public void runActivity(@NotNull Project project) {
        String autoUploadProp = PropertyUtil.getPropertyValue(PROPERTY_AUTO_UPLOAD, project);

        if (autoUploadProp != null && autoUploadProp.equals("false")) {
            return;
        }

        FileChangeListener fileChangeListener = new FileChangeListener(project);
        fileChangeListener.initComponent();

        LOGGER.info("on");

        //config validation
        new Crowdin(project);
    }
}
