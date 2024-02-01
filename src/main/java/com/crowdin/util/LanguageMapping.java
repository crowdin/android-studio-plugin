package com.crowdin.util;

import java.util.HashMap;
import java.util.Map;

public class LanguageMapping {

    private final Map<String, Map<String, String>> languageMapping;

    private LanguageMapping() {
        this.languageMapping = new HashMap<>();
    }

    public boolean containsValue(String langCode, String placeholder) {
        return languageMapping.containsKey(langCode) && languageMapping.get(langCode).containsKey(placeholder);
    }

    public String getValue(String langCode, String placeholder) {
        if (!this.containsValue(langCode, placeholder)) {
            return null;
        } else {
            return languageMapping.get(langCode).get(placeholder);
        }
    }

    public String getValueOrDefault(String langCode, String placeholder, String defaultValue) {
        String value = getValue(langCode, placeholder);
        return (value != null) ? value : defaultValue;
    }

    private LanguageMapping(Map<String, Map<String, String>> languageMapping) {
        this.languageMapping = languageMapping;
    }

    /**
     * Server language mapping. It has the following structure:
     * {
     *     "uk": {
     *         "name": "Ukrainian",
     *         "two_letters_code": "ua"
     *     }
     * }
     * @param serverLanguageMapping language mapping from server. May be null/empty
     * @return immutable LanguageMapping object
     */
    public static LanguageMapping fromServerLanguageMapping(Map<String, Map<String, String>> serverLanguageMapping) {
        return (serverLanguageMapping == null)
            ? new LanguageMapping()
            : new LanguageMapping(deepCopy(serverLanguageMapping));
    }

    private static Map<String, Map<String, String>> deepCopy(Map<String, Map<String, String>> toCopy) {
        Map<String, Map<String, String>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : toCopy.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    @Override
    public String toString() {
        return "LanguageMapping{" +
                "languageMapping=" + languageMapping +
                '}';
    }
}
