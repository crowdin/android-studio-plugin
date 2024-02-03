package com.crowdin.client.config;

import com.crowdin.util.FileUtil;
import com.crowdin.util.StringUtils;
import com.crowdin.util.Util;
import com.intellij.openapi.project.Project;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.crowdin.Constants.*;

public class CrowdinPropertiesLoader {

    private static final String STANDARD_TRANSLATION_PATTERN = "/values-%android_code%/%original_file_name%";
    private static final String STANDARD_SOURCE_DIRECTORY = "values";
    private static final String STANDARD_SOURCE_PATH = "**/" + STANDARD_SOURCE_DIRECTORY + "/";
    private static final String STANDARD_SOURCE_NAME = "strings.xml";
    private static final String STANDARD_SOURCE_FILE_PATH = STANDARD_SOURCE_PATH + STANDARD_SOURCE_NAME;
    private static final String PROJECT_ID = "project-id";
    private static final String PROJECT_ID_ENV = "project-id-env";
    private static final String API_TOKEN = "api-token";
    private static final String API_TOKEN_ENV = "api-token-env";
    private static final String BASE_URL = "base-url";
    private static final String BASE_URL_ENV = "base-url-env";
    private static final String PROPERTY_SOURCES = "sources";
    private static final String PROPERTY_LABELS = "labels";
    private static final String PROPERTY_FILES = "files";
    private static final String PROPERTY_FILES_SOURCE = "source";
    private static final String PROPERTY_FILES_TRANSLATION = "translation";
    private static final String PROPERTY_FILES_CLEANUP_MODE = "cleanup-mode";
    private static final String PROPERTY_FILES_UPDATE_STRINGS = "update-strings";
    private static final String PROPERTY_EXCLUDED_TARGET_LANGUAGES = "excluded-target-languages";
    private static final String PROPERTY_DISABLE_BRANCHES = "disable-branches";
    private static final String PROPERTY_PRESERVE_HIERARCHY = "preserve-hierarchy";
    private static final String PROPERTY_DEBUG = "debug";
    private static final String PROPERTY_AUTOCOMPLETION_DISABLED = "completion-disabled";
    private static final String PROPERTY_AUTOCOMPLETION_FILE_EXTENSIONS = "completion-file-extensions";
    private static final String PROPERTY_IMPORT_EQ_SUGGESTIONS = "import-eq-suggestions";
    private static final String PROPERTY_AUTO_APPROVE_IMPORTED = "auto-approve-imported";
    private static final String PROPERTY_TRANSLATE_HIDDEN = "translate-hidden";

    private static final Pattern BASE_URL_PATTERN = Pattern.compile("^(https://([a-zA-Z0-9_-]+\\.)?crowdin\\.com/?|http://(.+)\\.dev\\.crowdin\\.com/?)$");

    public static CrowdinConfig load(Project project) {
        Map<String, Object> properties = CrowdinFileProvider.load(project);
        return CrowdinPropertiesLoader.load(properties);
    }

    protected static CrowdinConfig load(Map<String, Object> properties) {
        List<String> errors = new ArrayList<>();
        List<String> notExistEnvVars = new ArrayList<>();
        CrowdinConfig crowdinProperties = new CrowdinConfig();
        if (properties == null) {
            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_config_file"), CONFIG_FILE));
        } else {
            try {
                Object propProjectId = properties.get(PROJECT_ID);
                String propProjectIdEnv = (String) properties.get(PROJECT_ID_ENV);

                if (propProjectId != null) {
                    try {
                        crowdinProperties.setProjectId(Long.parseLong(propProjectId.toString()));
                    } catch (NumberFormatException e) {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.property_is_not_number"), PROJECT_ID));
                    }
                } else if (!StringUtils.isEmpty(propProjectIdEnv)) {
                    String propProjectIdEnvValue = System.getenv(propProjectIdEnv);
                    if (propProjectIdEnvValue == null) {
                        notExistEnvVars.add(propProjectIdEnv);
                    } else {
                        try {
                            crowdinProperties.setProjectId(Long.valueOf(propProjectIdEnvValue));
                        } catch (NumberFormatException e) {
                            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.env_property_is_not_number"), propProjectIdEnv));
                        }
                    }
                } else {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROJECT_ID));
                }

                String propApiToken = (String) properties.get(API_TOKEN);
                String propApiTokenEnv = (String) properties.get(API_TOKEN_ENV);
                if (!StringUtils.isEmpty(propApiToken)) {
                    crowdinProperties.setApiToken(propApiToken);
                } else if (!StringUtils.isEmpty(propApiTokenEnv)) {
                    String propApiTokenEnvValue = System.getenv(propApiTokenEnv);
                    if (propApiTokenEnvValue != null) {
                        crowdinProperties.setApiToken(propApiTokenEnvValue);
                    } else {
                        notExistEnvVars.add(propApiTokenEnv);
                    }
                } else {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), API_TOKEN));
                }

                String propBaseUrl = (String) properties.get(BASE_URL);
                String propBaseUrlEnv = (String) properties.get(BASE_URL_ENV);

                if (!StringUtils.isEmpty(propBaseUrl)) {
                    if (isBaseUrlValid(propBaseUrl)) {
                        crowdinProperties.setBaseUrl(propBaseUrl);
                    } else {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_property"), BASE_URL));
                    }
                } else if (!StringUtils.isEmpty(propBaseUrlEnv)) {
                    String propBaseUrlEnvValue = System.getenv(propBaseUrlEnv);
                    if (propBaseUrlEnvValue == null) {
                        notExistEnvVars.add(propBaseUrlEnv);
                    } else if (!isBaseUrlValid(propBaseUrlEnvValue)) {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_env"), propBaseUrlEnv, propBaseUrlEnvValue));
                    } else {
                        crowdinProperties.setBaseUrl(propBaseUrlEnvValue);
                    }
                }

                if (notExistEnvVars.size() == 1) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.sysenv_not_exist.single"), notExistEnvVars.get(0)));
                } else if (!notExistEnvVars.isEmpty()) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.sysenv_not_exist.plural"), String.join(", ", notExistEnvVars)));
                }

                Boolean disabledBranches = (Boolean) properties.get(PROPERTY_DISABLE_BRANCHES);
                crowdinProperties.setDisabledBranches(disabledBranches);
                Boolean preserveHierarchy = (Boolean) properties.get(PROPERTY_PRESERVE_HIERARCHY);
                crowdinProperties.setPreserveHierarchy(preserveHierarchy);
                Boolean debug = (Boolean) properties.get(PROPERTY_DEBUG);
                crowdinProperties.setDebug(debug);
                crowdinProperties.setFiles(getFileBeans(properties, errors));

                Boolean autocompletionDisabled = (Boolean) properties.get(PROPERTY_AUTOCOMPLETION_DISABLED);
                List<String> autocompletionFileExtensions = (List<String>) properties.get(PROPERTY_AUTOCOMPLETION_FILE_EXTENSIONS);
                crowdinProperties.setAutocompletionDisabled(autocompletionDisabled);
                crowdinProperties.setAutocompletionFileExtensions(autocompletionFileExtensions);

                Boolean importEqSuggestions = (Boolean) properties.get(PROPERTY_IMPORT_EQ_SUGGESTIONS);
                crowdinProperties.setImportEqSuggestions(importEqSuggestions);
                Boolean autoApproveImported = (Boolean) properties.get(PROPERTY_AUTO_APPROVE_IMPORTED);
                crowdinProperties.setAutoApproveImported(autoApproveImported);
                Boolean translateHidden = (Boolean) properties.get(PROPERTY_TRANSLATE_HIDDEN);
                crowdinProperties.setTranslateHidden(translateHidden);

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

    private static List<FileBean> getFileBeans(Map<String, Object> properties, List<String> errors) {
        List<FileBean> fileBeans = getSourcesList(properties);
        fileBeans.addAll(getSourcesWithTranslations(properties, errors));
        if (fileBeans.isEmpty()) {
            FileBean defaultFileBean = new FileBean();
            defaultFileBean.setSource(STANDARD_SOURCE_FILE_PATH);
            defaultFileBean.setTranslation(STANDARD_TRANSLATION_PATTERN);
            fileBeans.add(defaultFileBean);
        }
        List<String> labels = (List<String>) properties.get(PROPERTY_LABELS);
        List<String> excluded_target_languages = (List<String>) properties.get(PROPERTY_EXCLUDED_TARGET_LANGUAGES);
        for (FileBean fb : fileBeans) {
            if (fb.getLabels() == null) {
                fb.setLabels(labels);
            }
            if (fb.getExcludedTargetLanguages() == null) {
                fb.setExcludedTargetLanguages(excluded_target_languages);
            }
        }
        return fileBeans;
    }

    private static List<FileBean> getSourcesList(Map<String, Object> properties) {
        List<String> sources = (List<String>) properties.get(PROPERTY_SOURCES);
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }
        return sources.stream()
                .map(String::trim)
                .filter(e -> !StringUtils.isEmpty(e))
                .map(s -> STANDARD_SOURCE_PATH + s)
                .map(source -> {
                    FileBean fb = new FileBean();
                    fb.setSource(source);
                    fb.setTranslation(STANDARD_TRANSLATION_PATTERN);
                    return fb;
                })
                .collect(Collectors.toList());
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