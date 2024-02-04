package com.crowdin.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@State(name = "CrowdinSettingsState", storages = @Storage("CrowdinSettingsPlugin.xml"))
public class CrowdingSettingsState implements PersistentStateComponent<CrowdingSettingsState> {

    public String projectId;
    public String baseUrl;
    public String fileExtensions;
    public boolean doNotShowConfirmation = false;
    public boolean disableBranches = false;
    public boolean autoUpload = true;
    public boolean disableCompletion = false;

    public String getApiToken() {
        Credentials credentials = PasswordSafe.getInstance().get(this.credentialAttributes());
        return Optional.ofNullable(credentials).map(Credentials::getPasswordAsString).orElse(null);
    }

    public void saveApiToken(String token) {
        PasswordSafe.getInstance().set(this.credentialAttributes(), new Credentials(token, token));
    }

    private CredentialAttributes credentialAttributes() {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("Crowdin", "apiToken"));
    }

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
