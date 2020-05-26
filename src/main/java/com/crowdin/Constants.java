package com.crowdin;

import java.util.regex.Pattern;

public final class Constants {

    public static final String STANDARD_TRANSLATION_PATTERN = "/values-%android_code%/%original_file_name%";
    public static final String STANDARD_SOURCE_PATH = "values";
    public static final String STANDARD_SOURCE_NAME = "strings.xml";

    public static final String PROPERTIES_FILE = "crowdin.properties";

    public static final String PROJECT_ID = "project-id";
    public static final String API_TOKEN = "api-token";
    public static final String BASE_URL = "base-url";
    public static final String PROPERTY_SOURCES = "sources";
    public static final String PROPERTY_FILES_SOURCES_PATTERN = "files.%ssource";
    public static final String PROPERTY_FILES_TRANSLATIONS_PATTERN = "files.%stranslation";
    public static final String PROPERTY_AUTO_UPLOAD = "auto-upload";
    public static final String PROPERTY_DISABLE_BRANCHES = "disable-branches";

    public static final Pattern BASE_URL_PATTERN = Pattern.compile("^(https|http)://(.+\\.)?crowdin\\.com/?$");
}
