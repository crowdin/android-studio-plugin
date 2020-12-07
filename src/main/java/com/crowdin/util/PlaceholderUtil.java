package com.crowdin.util;

import com.crowdin.client.languages.model.Language;
import lombok.NonNull;
import org.apache.commons.io.FilenameUtils;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class PlaceholderUtil {

    protected static final String PLACEHOLDER_LANGUAGE_ID = "%language_id%";
    private static final String PLACEHOLDER_ANDROID_CODE = "%android_code%";
    private static final String PLACEHOLDER_LANGUAGE = "%language%";
    private static final String PLACEHOLDER_LOCALE = "%locale%";
    private static final String PLACEHOLDER_LOCALE_WITH_UNDERSCORE = "%locale_with_underscore%";
    private static final String PLACEHOLDER_THREE_LETTERS_CODE = "%three_letters_code%";
    private static final String PLACEHOLDER_TWO_LETTERS_CODE = "%two_letters_code%";
    private static final String PLACEHOLDER_OSX_CODE = "%osx_code%";
    private static final String PLACEHOLDER_OSX_LOCALE = "%osx_locale%";

    protected static final String PLACEHOLDER_LANGUAGE_ID_NAME = "language_id";
    private static final String PLACEHOLDER_ANDROID_CODE_NAME = "android_code";
    private static final String PLACEHOLDER_LANGUAGE_NAME = "language";
    private static final String PLACEHOLDER_LANGUAGE_NAME_2 = "name";
    private static final String PLACEHOLDER_LOCALE_NAME = "locale";
    private static final String PLACEHOLDER_LOCALE_WITH_UNDERSCORE_NAME = "locale_with_underscore";
    private static final String PLACEHOLDER_THREE_LETTERS_CODE_NAME = "three_letters_code";
    private static final String PLACEHOLDER_TWO_LETTERS_CODE_NAME = "two_letters_code";
    private static final String PLACEHOLDER_OSX_CODE_NAME = "osx_code";
    private static final String PLACEHOLDER_OSX_LOCALE_NAME = "osx_locale";

    private static final String PLACEHOLDER_FILE_EXTENSION = "%file_extension%";
    private static final String PLACEHOLDER_FILE_NAME = "%file_name%";
    private static final String PLACEHOLDER_ORIGINAL_FILE_NAME = "%original_file_name%";
    private static final String PLACEHOLDER_ORIGINAL_PATH = "%original_path%";

    private PlaceholderUtil() {}

    /**
     * Build relative paths to all possible translations.
     *
     * @param relativeSourcePath source path relative to project root (StringUtils.removeStart(source.getPath(), root.getPath())
     * @param translationPattern translation pattern
     * @param projLanguages list of project languages
     * @return relative paths to all possible translations
     */
    public static Map<Language, String> buildTranslationPatterns(
        String relativeSourcePath, String translationPattern, List<Language> projLanguages, LanguageMapping languageMapping
    ) {
        String basePattern = PlaceholderUtil.replaceFilePlaceholders(translationPattern, relativeSourcePath);
        return projLanguages.stream()
            .collect(toMap(lang -> lang, lang -> PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang, languageMapping)));
    }

    public static String replaceLanguagePlaceholders(@NonNull String pattern, @NonNull Language lang, LanguageMapping langMapping) {
        return pattern
            .replaceAll(PLACEHOLDER_LANGUAGE_ID,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_LANGUAGE_ID_NAME, lang.getId()))
            .replace(PLACEHOLDER_LANGUAGE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_LANGUAGE_NAME,
                    langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_LANGUAGE_NAME_2, lang.getName())))
            .replace(PLACEHOLDER_LOCALE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_LOCALE_NAME, lang.getLocale()))
            .replace(PLACEHOLDER_LOCALE_WITH_UNDERSCORE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_LOCALE_WITH_UNDERSCORE_NAME, lang.getLocale().replace("-", "_")))
            .replace(PLACEHOLDER_TWO_LETTERS_CODE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_TWO_LETTERS_CODE_NAME, lang.getTwoLettersCode()))
            .replace(PLACEHOLDER_THREE_LETTERS_CODE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_THREE_LETTERS_CODE_NAME, lang.getThreeLettersCode()))
            .replace(PLACEHOLDER_ANDROID_CODE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_ANDROID_CODE_NAME, lang.getAndroidCode()))
            .replace(PLACEHOLDER_OSX_LOCALE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_OSX_LOCALE_NAME, lang.getOsxLocale()))
            .replace(PLACEHOLDER_OSX_CODE,
                langMapping.getValueOrDefault(lang.getId(), PLACEHOLDER_OSX_CODE_NAME, lang.getOsxCode()));
    }

    public static String replaceFilePlaceholders(@NonNull String toFormat, @NonNull String sourcePath) {
        return toFormat
            .replace(PLACEHOLDER_ORIGINAL_FILE_NAME, FilenameUtils.getName(sourcePath))
            .replace(PLACEHOLDER_FILE_NAME, FilenameUtils.getBaseName(sourcePath))
            .replace(PLACEHOLDER_FILE_EXTENSION, FilenameUtils.getExtension(sourcePath))
            .replace(PLACEHOLDER_ORIGINAL_PATH, FilenameUtils.getPathNoEndSeparator(sourcePath));
    }
}
