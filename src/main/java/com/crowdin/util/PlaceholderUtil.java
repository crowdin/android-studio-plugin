package com.crowdin.util;

import com.crowdin.client.languages.model.Language;
import lombok.NonNull;
import org.apache.commons.io.FilenameUtils;

public class PlaceholderUtil {

    private static final String PLACEHOLDER_ANDROID_CODE = "%android_code%";
    private static final String PLACEHOLDER_LANGUAGE = "%language%";
    private static final String PLACEHOLDER_LOCALE = "%locale%";
    private static final String PLACEHOLDER_LOCALE_WITH_UNDERSCORE = "%locale_with_underscore%";
    private static final String PLACEHOLDER_THREE_LETTERS_CODE = "%three_letters_code%";
    private static final String PLACEHOLDER_TWO_LETTERS_CODE = "%two_letters_code%";
    private static final String PLACEHOLDER_OSX_CODE = "%osx_code%";
    private static final String PLACEHOLDER_OSX_LOCALE = "%osx_locale%";

    private static final String PLACEHOLDER_FILE_EXTENSION = "%file_extension%";
    private static final String PLACEHOLDER_FILE_NAME = "%file_name%";
    private static final String PLACEHOLDER_ORIGINAL_FILE_NAME = "%original_file_name%";
    private static final String PLACEHOLDER_ORIGINAL_PATH = "%original_path%";

    private PlaceholderUtil() {}

    public static String replaceLanguagePlaceholders(@NonNull String pattern, @NonNull Language lang) {
        return pattern
            .replace(PLACEHOLDER_LANGUAGE, lang.getName())
            .replace(PLACEHOLDER_LOCALE, lang.getLocale())
            .replace(PLACEHOLDER_LOCALE_WITH_UNDERSCORE, lang.getLocale().replace("-", "_"))
            .replace(PLACEHOLDER_TWO_LETTERS_CODE, lang.getTwoLettersCode())
            .replace(PLACEHOLDER_THREE_LETTERS_CODE, lang.getThreeLettersCode())
            .replace(PLACEHOLDER_ANDROID_CODE, lang.getAndroidCode())
            .replace(PLACEHOLDER_OSX_LOCALE, lang.getOsxLocale())
            .replace(PLACEHOLDER_OSX_CODE, lang.getOsxCode());
    }

    public static String replaceFilePlaceholders(@NonNull String toFormat, @NonNull String sourcePath) {
        return toFormat
            .replace(PLACEHOLDER_ORIGINAL_FILE_NAME, FilenameUtils.getName(sourcePath))
            .replace(PLACEHOLDER_FILE_NAME, FilenameUtils.getBaseName(sourcePath))
            .replace(PLACEHOLDER_FILE_EXTENSION, FilenameUtils.getExtension(sourcePath))
            .replace(PLACEHOLDER_ORIGINAL_PATH, FilenameUtils.getPathNoEndSeparator(sourcePath));
    }
}
