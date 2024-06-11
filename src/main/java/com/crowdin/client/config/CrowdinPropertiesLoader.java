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
import java.util.stream.Collectors;

import static com.crowdin.Constants.CONFIG_FILE;
import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class CrowdinPropertiesLoader {

    private static final String PROJECT_ID = "project_id";
    private static final String PROJECT_ID_ENV = "project_id_env";
    private static final String API_TOKEN = "api_token";
    private static final String API_TOKEN_ENV = "api_token_env";
    private static final String BASE_URL = "base_url";
    private static final String BASE_URL_ENV = "base_url_env";
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

    private static final Pattern BASE_URL_PATTERN = Pattern.compile("^(https://([a-zA-Z0-9_-]+\\.(api\\.)?)?crowdin\\.com/?|http://(.+)\\.dev\\.crowdin\\.com/?)$");

    public static boolean isWorkspaceNotPrepared(Project project) {
        return CrowdinFileProvider.getCrowdinConfigFile(project) == null;
    }

    public static CrowdinConfig load(Project project) {
        CrowdingSettingsState settings = CrowdingSettingsState.getInstance(project);
        Map<String, Object> properties = CrowdinFileProvider.load(project);
        return CrowdinPropertiesLoader.load(properties, settings);
    }

    protected static CrowdinConfig load(Map<String, Object> properties, CrowdingSettingsState settings) {
        List<String> errors = new ArrayList<>();
        List<String> notExistEnvVars = new ArrayList<>();
        CrowdinConfig crowdinProperties = new CrowdinConfig();
        if (properties == null) {
            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_config_file"), CONFIG_FILE));
        } else {
            try {

                Object propProjectId = properties.get(PROJECT_ID);
                String propProjectIdEnv = getStringProperty(properties, PROJECT_ID_ENV);

                if (propProjectId != null) {
                    try {
                        crowdinProperties.setProjectId(Long.parseLong(propProjectId.toString()));
                    } catch (NumberFormatException e) {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.project_id_format"), PROJECT_ID));
                    }
                } else if (!StringUtils.isEmpty(propProjectIdEnv)) {
                    String propProjectIdEnvValue = System.getenv(propProjectIdEnv);
                    if (propProjectIdEnvValue == null) {
                        notExistEnvVars.add(propProjectIdEnv);
                    } else {
                        try {
                            crowdinProperties.setProjectId(Long.valueOf(propProjectIdEnvValue));
                        } catch (NumberFormatException e) {
                            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.project_id_format"), propProjectIdEnv));
                        }
                    }
                } else if (!StringUtils.isEmpty(settings.projectId)) {
                    try {
                        crowdinProperties.setProjectId(Long.valueOf(settings.projectId));
                    } catch (NumberFormatException e) {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.project_id_format"), settings.projectId));
                    }
                } else {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROJECT_ID));
                }

                String propApiToken = getStringProperty(properties, API_TOKEN);
                String propApiTokenEnv = getStringProperty(properties, API_TOKEN_ENV);
                if (!StringUtils.isEmpty(propApiToken)) {
                    crowdinProperties.setApiToken(propApiToken);
                } else if (!StringUtils.isEmpty(propApiTokenEnv)) {
                    String propApiTokenEnvValue = System.getenv(propApiTokenEnv);
                    if (propApiTokenEnvValue != null) {
                        crowdinProperties.setApiToken(propApiTokenEnvValue);
                    } else {
                        notExistEnvVars.add(propApiTokenEnv);
                    }
                } else if (!StringUtils.isEmpty(settings.getApiToken())) {
                    crowdinProperties.setApiToken(settings.getApiToken());
                } else {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), API_TOKEN));
                }

                String propBaseUrl = getStringProperty(properties, BASE_URL);
                String propBaseUrlEnv = getStringProperty(properties, BASE_URL_ENV);
                if (!StringUtils.isEmpty(propBaseUrl)) {
                    if (isBaseUrlValid(propBaseUrl)) {
                        crowdinProperties.setBaseUrl(propBaseUrl);
                    } else {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_property"), propBaseUrl));
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
                } else if (!StringUtils.isEmpty(settings.baseUrl)) {
                    if (isBaseUrlValid(settings.baseUrl)) {
                        crowdinProperties.setBaseUrl(settings.baseUrl);
                    } else {
                        errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_property"), settings.baseUrl));
                    }
                }

                if (notExistEnvVars.size() == 1) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.sysenv_not_exist.single"), notExistEnvVars.get(0)));
                } else if (!notExistEnvVars.isEmpty()) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.sysenv_not_exist.plural"), String.join(", ", notExistEnvVars)));
                }

                crowdinProperties.setFiles(getSourcesWithTranslations(properties, errors));

                if (!StringUtils.isEmpty(settings.fileExtensions)) {
                    crowdinProperties.setAutocompletionFileExtensions(Arrays.asList(settings.fileExtensions.split(",")));
                }
                crowdinProperties.setAutocompletionEnabled(settings.enableCompletion);

                crowdinProperties.setUseGitBranch(settings.useGitBranch);
                Boolean preserveHierarchy = getBooleanProperty(properties, PROPERTY_PRESERVE_HIERARCHY);
                crowdinProperties.setPreserveHierarchy(preserveHierarchy);
                Boolean debug = getBooleanProperty(properties, PROPERTY_DEBUG);
                crowdinProperties.setDebug(debug);
                Boolean importEqSuggestions = getBooleanProperty(properties, PROPERTY_IMPORT_EQ_SUGGESTIONS);
                crowdinProperties.setImportEqSuggestions(importEqSuggestions);
                Boolean autoApproveImported = getBooleanProperty(properties, PROPERTY_AUTO_APPROVE_IMPORTED);
                crowdinProperties.setAutoApproveImported(autoApproveImported);
                Boolean translateHidden = getBooleanProperty(properties, PROPERTY_TRANSLATE_HIDDEN);
                crowdinProperties.setTranslateHidden(translateHidden);
                String branch = getStringProperty(properties, PROPERTY_BRANCH);
                crowdinProperties.setBranch(branch);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.config.has_errors") + " " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(Util.prepareListMessageText(MESSAGES_BUNDLE.getString("errors.config.has_errors"), errors));
        }

        return crowdinProperties;
    }

    private static List<FileBean> getSourcesWithTranslations(Map<String, Object> properties, List<String> errors) {
        List<FileBean> fileBeans = new ArrayList<>();

        List<Map<String, Object>> files = getListOfObjectsProperty(properties, PROPERTY_FILES);

        if (files != null) {
            files.forEach(file -> {
                String source = getStringProperty(file, PROPERTY_FILES_SOURCE);
                String translation = getStringProperty(file, PROPERTY_FILES_TRANSLATION);
                Boolean updateStrings = getBooleanProperty(file, PROPERTY_FILES_UPDATE_STRINGS);
                Boolean cleanupMode = getBooleanProperty(file, PROPERTY_FILES_CLEANUP_MODE);
                List<String> labels = getListStringsProperty(file, PROPERTY_LABELS);
                List<String> excludedTargetLanguages = getListStringsProperty(file, PROPERTY_EXCLUDED_TARGET_LANGUAGES);

                if (!StringUtils.isEmpty(source) && !StringUtils.isEmpty(translation)) {
                    FileBean fb = new FileBean();
                    fb.setSource(FileUtil.noSepAtStart(FileUtil.unixPath(source)));
                    fb.setTranslation(FileUtil.unixPath(translation));
                    fb.setLabels(labels);
                    fb.setExcludedTargetLanguages(excludedTargetLanguages);
                    fb.setUpdateStrings(updateStrings);
                    fb.setCleanupMode(cleanupMode);
                    fileBeans.add(fb);
                } else if (StringUtils.isEmpty(source)) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROPERTY_FILES_SOURCE));
                } else if (StringUtils.isEmpty(translation)) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROPERTY_FILES_TRANSLATION));
                }
            });
        }

        return fileBeans;
    }

    private static String getStringProperty(Map<String, Object> map, String property) {
        return getProperty(map, property, String.class);
    }

    private static List<String> getListStringsProperty(Map<String, Object> map, String property) {
        List list = getProperty(map, property, List.class);
        if (list == null) {
            return null;
        }
        List<String> res = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            try {
                res.add(String.class.cast(el));
            } catch (ClassCastException e) {
                throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_format"), String.format("%s[%s]", property, i)));
            }
        }
        return res;
    }

    private static List<Map<String, Object>> getListOfObjectsProperty(Map<String, Object> map, String property) {
        List list = getProperty(map, property, List.class);
        if (list == null) {
            return null;
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            try {
                Map<Object, Object> object = Map.class.cast(el);

                int finalI = i;
                Map<String, Object> castedObject = object
                        .entrySet()
                        .stream()
                        .peek(entry -> {
                            if (entry.getKey() == null || entry.getValue() == null) {
                                throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_format"), String.format("%s[%s]", property, finalI)));
                            }
                        })
                        .collect(Collectors.toMap(
                                field -> String.class.cast(field.getKey()),
                                Map.Entry::getValue
                        ));
                res.add(castedObject);
            } catch (ClassCastException e) {
                throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_format"), String.format("%s[%s]", property, i)));
            }
        }
        return res;
    }

    private static Boolean getBooleanProperty(Map<String, Object> map, String property) {
        return getProperty(map, property, Boolean.class);
    }

    private static <T> T getProperty(Map<String, Object> map, String property, Class<T> clazz) {
        try {
            Object value = map.get(property);
            if (value != null) {
                return clazz.cast(value);
            }
            return null;
        } catch (ClassCastException e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_format"), property));
        }
    }

    protected static boolean isBaseUrlValid(String baseUrl) {
        return BASE_URL_PATTERN.matcher(baseUrl).matches();
    }
}
