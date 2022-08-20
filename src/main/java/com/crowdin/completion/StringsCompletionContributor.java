package com.crowdin.completion;

import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.util.NotificationUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StringsCompletionContributor extends CompletionContributor {

    private final Icon icon;

    public StringsCompletionContributor() {
        icon = IconLoader.getIcon("/icons/icon.svg", this.getClass());
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Project project = parameters.getEditor().getProject();
        String extension = parameters.getOriginalFile().getVirtualFile().getExtension();

        CrowdinProperties properties;
        try {
            properties = CrowdinPropertiesLoader.load(project);
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
            return;
        }

        if (properties.isAutocompletionDisabled()) {
            return;
        }

        if (properties.getAutocompletionFileExtensions() != null &&
                properties.getAutocompletionFileExtensions().size() > 0 &&
                !properties.getAutocompletionFileExtensions().contains(extension)) {
            return;
        }

        //TODO get crowdin strings & cache
        result.addElement(LookupElementBuilder.create("Hello").withTypeText("qweqwe", true));
        result.addElement(LookupElementBuilder.create("qwe").withTypeText("ssss", false).withIcon(icon));
        result.addElement(LookupElementBuilder.create("rty").withIcon(icon));
    }
}
