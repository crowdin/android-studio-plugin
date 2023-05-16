package com.crowdin.completion;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcestrings.model.SourceString;
import com.crowdin.logic.BranchLogic;
import com.crowdin.util.NotificationUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class StringsCompletionContributor extends CompletionContributor {

    private final Icon icon;

    public StringsCompletionContributor() {
        icon = IconLoader.getIcon("/icons/icon.svg", this.getClass());
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Project project = parameters.getEditor().getProject();
        String extension = parameters.getOriginalFile().getVirtualFile().getExtension();

        if (project == null) {
            return;
        }

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

        Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

        BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
        String branchName = branchLogic.acquireBranchName(true);
        CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, false);

        List<SourceString> strings = crowdinProjectCache.getStrings();
        Branch branch = branchLogic.getBranch(crowdinProjectCache, false);

        if (strings == null) {
            return;
        }

        strings.stream()
                .filter(s -> s.getIdentifier() != null && s.getText() != null)
                .filter(s -> {
                    if (branch == null) {
                        return true;
                    }
                    return Objects.equals(s.getBranchId(), branch.getId());
                })
                .map(s -> {
                    if (s.getText() instanceof String) {
                        return LookupElementBuilder
                                .create(s.getIdentifier())
                                .withTypeText(s.getText().toString())
                                .withIcon(icon);
                    } else if (s.getText() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (Map<String, String>) s.getText();
                        Optional<String> text = Stream.of
                                (
                                        Optional.ofNullable(map.get("one")),
                                        Optional.ofNullable(map.get("zero")),
                                        Optional.ofNullable(map.get("two")),
                                        Optional.ofNullable(map.get("few")),
                                        Optional.ofNullable(map.get("many")),
                                        Optional.ofNullable(map.get("other"))
                                )
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .findFirst();
                        return text.map(value -> LookupElementBuilder
                                .create(s.getIdentifier())
                                .withTypeText(value)
                                .withIcon(icon)).orElse(null);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(result::addElement);
    }
}
