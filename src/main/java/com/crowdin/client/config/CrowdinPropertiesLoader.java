package com.crowdin.client.config;

import com.crowdin.settings.CrowdingSettingsState;
import com.crowdin.util.FileUtil;
import com.crowdin.util.StringUtils;
import com.crowdin.util.Util;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.crowdin.Constants.CONFIG_FILE;
import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class CrowdinPropertiesLoader {
    private static final String PROPERTY_PRESERVE_HIERARCHY = "preserve_hierarchy";
    private static final String PROPERTY_DEBUG = "debug";
    private static final String PROPERTY_IMPORT_EQ_SUGGESTIONS = "import_eq_suggestions";
    private static final String PROPERTY_AUTO_APPROVE_IMPORTED = "auto_approve_imported";
    private static final String PROPERTY_TRANSLATE_HIDDEN = "translate_hidden";
    private static final String PROPERTY_BRANCH = "branch";
    private static final String PROPERTY_FILES = "files";
    private static final String PROPERTY_FILES_SOURCE = "source";
    private static final String PROPERTY_FILES_TRANSLATION = "translation";
    private static final String PROPERTY_LABELS = "labels";
    private static final String PROPERTY_FILES_CLEANUP_MODE = "cleanup_mode";
    private static final String PROPERTY_FILES_UPDATE_STRINGS = "update_strings";
    private static final String PROPERTY_EXCLUDED_TARGET_LANGUAGES = "excluded_target_languages";

    private static final Pattern BASE_URL_PATTERN = Pattern.compile("^(https://([a-zA-Z0-9_-]+\\.)?crowdin\\.com/?|http://(.+)\\.dev\\.crowdin\\.com/?)$");

    public static boolean isWorkspaceNotPrepared(Project project) {
        CrowdingSettingsState settings = CrowdingSettingsState.getInstance(project);

        if (StringUtils.isEmpty(settings.projectId) || StringUtils.isEmpty(settings.getApiToken())) {
            return true;
        }

        return CrowdinFileProvider.getCrowdinConfigFile(project) == null;
    }

    public static CrowdinConfig load(Project project) {
        CrowdingSettingsState settings = CrowdingSettingsState.getInstance(project);

        if (StringUtils.isEmpty(settings.projectId)) {
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.settings.project_id_missing"));
        }

        if (StringUtils.isEmpty(settings.getApiToken())) {
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.settings.api_token_missing"));
        }

        Map<String, Object> properties = CrowdinFileProvider.load(project);
        return CrowdinPropertiesLoader.load(properties, settings);
    }

    protected static CrowdinConfig load(Map<String, Object> properties, CrowdingSettingsState settings) {
        List<String> errors = new ArrayList<>();
        CrowdinConfig crowdinProperties = new CrowdinConfig();
        if (properties == null) {
            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_config_file"), CONFIG_FILE));
        } else {
            try {
                String projectId = settings.projectId;
                try {
                    crowdinProperties.setProjectId(Long.valueOf(projectId));
                } catch (NumberFormatException e) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.project_id_format"), projectId));
                }

                crowdinProperties.setApiToken(settings.getApiToken());

                String baseUrl = settings.baseUrl;

                if (!StringUtils.isEmpty(baseUrl)) {
                    if (isBaseUrlValid(baseUrl)) {
                        crowdinProperties.setBaseUrl(baseUrl);
                    } else {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_property"), baseUrl));
                    }
                }

                crowdinProperties.setFiles(getSourcesWithTranslations(properties, errors));

                if (!StringUtils.isEmpty(settings.fileExtensions)) {
                    crowdinProperties.setAutocompletionFileExtensions(Arrays.asList(settings.fileExtensions.split(",")));
                }
                crowdinProperties.setAutocompletionDisabled(settings.disableCompletion);

                crowdinProperties.setDisabledBranches(settings.disableBranches);
                Boolean preserveHierarchy = (Boolean) properties.get(PROPERTY_PRESERVE_HIERARCHY);
                crowdinProperties.setPreserveHierarchy(preserveHierarchy);
                Boolean debug = (Boolean) properties.get(PROPERTY_DEBUG);
                crowdinProperties.setDebug(debug);
                Boolean importEqSuggestions = (Boolean) properties.get(PROPERTY_IMPORT_EQ_SUGGESTIONS);
                crowdinProperties.setImportEqSuggestions(importEqSuggestions);
                Boolean autoApproveImported = (Boolean) properties.get(PROPERTY_AUTO_APPROVE_IMPORTED);
                crowdinProperties.setAutoApproveImported(autoApproveImported);
                Boolean translateHidden = (Boolean) properties.get(PROPERTY_TRANSLATE_HIDDEN);
                crowdinProperties.setTranslateHidden(translateHidden);
                String branch = (String) properties.get(PROPERTY_BRANCH);
                crowdinProperties.setBranch(branch);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.config.has_errors") + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(Util.prepareListMessageText(MESSAGES_BUNDLE.getString("errors.config.has_errors"), errors));
        }

        return crowdinProperties;
    }

    private static List<FileBean> getSourcesWithTranslations(Map<String, Object> properties, List<String> errors) {
        List<FileBean> fileBeans = new ArrayList<>();

        List<Map<String, Object>> files = (List<Map<String, Object>>) properties.get(PROPERTY_FILES);

        if (files != null) {
            files.forEach(file -> {
                String source = (String) file.get(PROPERTY_FILES_SOURCE);
                String translation = (String) file.get(PROPERTY_FILES_TRANSLATION);
                Boolean updateStrings = (Boolean) file.get(PROPERTY_FILES_UPDATE_STRINGS);
                Boolean cleanupMode = (Boolean) file.get(PROPERTY_FILES_CLEANUP_MODE);
                List<String> labels = (List<String>) file.get(PROPERTY_LABELS);
                List<String> excludedTargetLanguages = (List<String>) file.get(PROPERTY_EXCLUDED_TARGET_LANGUAGES);

                if (!StringUtils.isEmpty(source) && !StringUtils.isEmpty(translation)) {
                    FileBean fb = new FileBean();
                    fb.setSource(FileUtil.noSepAtStart(FileUtil.unixPath(source)));
                    fb.setTranslation(FileUtil.unixPath(translation));
                    fb.setLabels(labels);
                    fb.setExcludedTargetLanguages(excludedTargetLanguages);
                    fb.setUpdateStrings(updateStrings);
                    fb.setCleanupMode(cleanupMode);
                    fileBeans.add(fb);
                } else if (StringUtils.isEmpty(translation)) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROPERTY_FILES_SOURCE));
                } else if (StringUtils.isEmpty(source)) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROPERTY_FILES_TRANSLATION));
                }
            });
        }

        return fileBeans;
    }

    protected static boolean isBaseUrlValid(String baseUrl) {
        return BASE_URL_PATTERN.matcher(baseUrl).matches();
    }
}
