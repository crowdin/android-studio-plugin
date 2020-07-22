package com.crowdin.logic;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Data;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@State(name = "CrowdinSettings", storages = @Storage("crowdin_settings.xml"))
public class CrowdinSettings implements PersistentStateComponent<Element> {

    private static final String CROWDIN_SETTINGS_TAG = "CrowdinSettings";
    private static final String DO_NOT_SHOW_CONFIRMS = "DoNotShowConfirms";

    private boolean doNotShowConfirms;

    @Nullable
    @Override
    public Element getState() {
        Element element = new Element(CROWDIN_SETTINGS_TAG);
        element.setAttribute(DO_NOT_SHOW_CONFIRMS, Boolean.toString(this.isDoNotShowConfirms()));
        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        try {
            setDoNotShowConfirms(getBooleanValue(state, DO_NOT_SHOW_CONFIRMS));
        } catch (Exception e) {
            throw new RuntimeException("Error while loading crowdin settings", e);
        }
    }

    private boolean getBooleanValue(Element element, String attributeName) {
        String attributeValue = element.getAttributeValue(attributeName);
        if (attributeValue != null) {
            return Boolean.valueOf(attributeValue);
        } else {
            return false;
        }
    }


}
