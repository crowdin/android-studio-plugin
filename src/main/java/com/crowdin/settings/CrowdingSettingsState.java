package com.crowdin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "CrowdinSettingsState", storages = @Storage("CrowdinSettingsPlugin.xml"))
public class CrowdingSettingsState implements PersistentStateComponent<CrowdingSettingsState> {

    //TODO use these settings instead of properties
    public String projectId;
    //TODO use https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html
    public String apiToken;
    public String baseUrl;
    public String fileExtensions;
    public Boolean doNotShowConfirmation;
    public Boolean disableBranches;
    public Boolean autoUpload;
    public Boolean disableCompletion;

    public static CrowdingSettingsState getInstance(Project project) {
        return project.getService(CrowdingSettingsState.class);
    }

    @Override
    public CrowdingSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CrowdingSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
