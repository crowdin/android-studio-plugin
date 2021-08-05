package com.crowdin.client;

import com.crowdin.util.FileUtil;
import com.crowdin.util.PropertyUtil;
import com.crowdin.util.Util;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.crowdin.Constants.*;

public class CrowdinPropertiesLoader {


    public static CrowdinProperties load(Project project) {
        Properties properties = PropertyUtil.getProperties(project);
        return CrowdinPropertiesLoader.load(properties);
    }

    public static CrowdinProperties load(Properties properties) {
        List<String> errors = new ArrayList<>();
        List<String> notExistEnvVars = new ArrayList<>();
        CrowdinProperties crowdinProperties = new CrowdinProperties();
        if (properties == null) {
            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_config_file"), PROPERTIES_FILE));
        } else {
            String propProjectId = properties.getProperty(PROJECT_ID);
            String propProjectIdEnv = properties.getProperty(PROJECT_ID_ENV);
            if (StringUtils.isNotEmpty(propProjectId)) {
                try {
                    crowdinProperties.setProjectId(Long.valueOf(propProjectId));
                } catch (NumberFormatException e) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.property_is_not_number"), PROJECT_ID));
                }
            } else if (StringUtils.isNotEmpty(propProjectIdEnv)) {
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
            String propApiToken = properties.getProperty(API_TOKEN);
            String propApiTokenEnv = properties.getProperty(API_TOKEN_ENV);
            if (StringUtils.isNotEmpty(propApiToken)) {
                crowdinProperties.setApiToken(propApiToken);
            } else if (StringUtils.isNotEmpty(propApiTokenEnv)) {
                String propApiTokenEnvValue = System.getenv(propApiTokenEnv);
                if (propApiTokenEnvValue != null) {
                    crowdinProperties.setApiToken(propApiTokenEnvValue);
                } else {
                    notExistEnvVars.add(propApiTokenEnv);
                }
            } else {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), API_TOKEN));
            }
            String propBaseUrl = properties.getProperty(BASE_URL);
            String propBaseUrlEnv = properties.getProperty(BASE_URL_ENV);
            if (StringUtils.isNotEmpty(propBaseUrl)) {
                if (isBaseUrlValid(propBaseUrl)) {
                    crowdinProperties.setBaseUrl(propBaseUrl);
                } else {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_property"), BASE_URL));
                }
            } else if (StringUtils.isNotEmpty(propBaseUrlEnv)) {
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
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.sysenv_not_exist.plural"), StringUtils.join(notExistEnvVars, ", ")));
            }
            String disabledBranches = properties.getProperty(PROPERTY_DISABLE_BRANCHES);
            if (disabledBranches != null) {
                crowdinProperties.setDisabledBranches(Boolean.parseBoolean(disabledBranches));
            } else {
                crowdinProperties.setDisabledBranches(DISABLE_BRANCHES_DEFAULT);
            }
            String preserveHierarchy = properties.getProperty(PROPERTY_PRESERVE_HIERARCHY);
            if (preserveHierarchy != null) {
                crowdinProperties.setPreserveHierarchy(Boolean.parseBoolean(preserveHierarchy));
            } else {
                crowdinProperties.setPreserveHierarchy(PRESERVE_HIERARCHY_DEFAULT);
            }
            String debug = properties.getProperty(PROPERTY_DEBUG);
            if (debug != null) {
                crowdinProperties.setDebug(Boolean.parseBoolean(debug));
            } else {
                crowdinProperties.setDebug(false);
            }
            crowdinProperties.setFiles(getFileBeans(properties, errors));
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(Util.prepareListMessageText(MESSAGES_BUNDLE.getString("errors.config.has_errors"), errors));
        }

        return crowdinProperties;
    }

    private static List<FileBean> getFileBeans(Properties properties, List<String> errors) {
        List<FileBean> fileBeans = getSourcesList(properties);
        fileBeans.addAll(getSourcesWithTranslations(properties, errors));
        if (fileBeans.isEmpty()) {
            FileBean defaultFileBean = new FileBean();
            defaultFileBean.setSource(STANDARD_SOURCE_FILE_PATH);
            defaultFileBean.setTranslation(STANDARD_TRANSLATION_PATTERN);
            fileBeans.add(defaultFileBean);
        }
        List<String> labels = parsePropertyToList(properties.getProperty(PROPERTY_LABELS));
        List<String> excluded_target_languages = parsePropertyToList(properties.getProperty(PROPERTY_EXCLUDED_TARGET_LANGUAGES));
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

    private static List<FileBean> getSourcesList(Properties properties) {
        String sources = properties.getProperty(PROPERTY_SOURCES);
        if (sources == null || StringUtils.isEmpty(sources)) {
            return new ArrayList<>();
        }
        return Arrays.stream(sources.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .map(s -> STANDARD_SOURCE_PATH + s)
            .map(source -> {
                FileBean fb = new FileBean();
                fb.setSource(source);
                fb.setTranslation(STANDARD_TRANSLATION_PATTERN);
                return fb;
            })
            .collect(Collectors.toList());
    }

    private static List<FileBean> getSourcesWithTranslations(Properties properties, List<String> errors) {
        List<FileBean> fileBeans = new ArrayList<>();

        List<String> foundKeys = properties.keySet().stream()
            .map(key -> (String) key)
            .filter(PROPERTY_FILES_SOURCES_REGEX.asPredicate()
                .or(PROPERTY_FILES_TRANSLATIONS_REGEX.asPredicate()))
            .map(key -> StringUtils.removeStart(key, "files."))
            .map(key -> StringUtils.removeEnd(key, "source"))
            .map(key -> StringUtils.removeEnd(key, "translation"))
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        for (String ident : foundKeys) {
            String source = properties.getProperty(String.format(PROPERTY_FILES_SOURCES_PATTERN, ident));
            String translation = properties.getProperty(String.format(PROPERTY_FILES_TRANSLATIONS_PATTERN, ident));
            List<String> labels = parsePropertyToList(properties.getProperty(String.format(PROPERTY_FILES_LABELS_PATTERN, ident)));
            List<String> excludedTargetLanguages = parsePropertyToList(properties.getProperty(String.format(PROPERTY_FILES_EXCLUDED_TARGET_LANGUAGES_PATTERN, ident)));
            if (StringUtils.isNotEmpty(source) && StringUtils.isNotEmpty(translation)) {
                FileBean fb = new FileBean();
                fb.setSource(FileUtil.noSepAtStart(FileUtil.unixPath(source)));
                fb.setTranslation(FileUtil.unixPath(translation));
                fb.setLabels(labels);
                fb.setExcludedTargetLanguages(excludedTargetLanguages);
                fileBeans.add(fb);
            } else if (StringUtils.isEmpty(translation)) {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), String.format(PROPERTY_FILES_TRANSLATIONS_PATTERN, ident)));
            } else if (StringUtils.isEmpty(source)) {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), String.format(PROPERTY_FILES_SOURCES_PATTERN, ident)));
            }
        }
        return fileBeans;
    }

    private static List<String> parsePropertyToList(String property) {
        if (StringUtils.isEmpty(property)) {
            return null;
        }
        List<String> parsedProperty = new ArrayList<>();
        for (String part : property.split(",")) {
            part = part.trim();
            if (StringUtils.isNotEmpty(part)) {
                parsedProperty.add(part);
            }
        }
        return parsedProperty;
    }

    protected static boolean isBaseUrlValid(String baseUrl) {
        return BASE_URL_PATTERN.matcher(baseUrl).matches();
    }
}
