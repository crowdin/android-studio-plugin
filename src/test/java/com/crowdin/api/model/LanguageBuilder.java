package com.crowdin.api.model;

import com.crowdin.client.languages.model.Language;

public enum LanguageBuilder {
    UKR("Ukrainian", "uk", "uk", "ukr", "uk-UA", "uk-rUA", "uk", "uk.lproj"),
    RUS("Russian", "ru", "ru", "rus", "ru-RU", "ru-rRU", "ru", "ru.lproj"),
    DEU("German", "de", "de", "deu", "de-DE", "de-rDE", "de", "de.proj"),
    ENG("English", "en", "en", "eng", "en-US", "en-rUS", "en", "en.lproj");

    private final Language lang;

    LanguageBuilder(
            String name,
            String id,
            String twoLettersCode,
            String threeLettersCode,
            String locale,
            String androidCode,
            String osxLocale,
            String osxCode
    ) {
        lang = new Language();
        lang.setName(name);
        lang.setId(id);
        lang.setTwoLettersCode(twoLettersCode);
        lang.setThreeLettersCode(threeLettersCode);
        lang.setLocale(locale);
        lang.setAndroidCode(androidCode);
        lang.setOsxLocale(osxLocale);
        lang.setOsxCode(osxCode);
    }

    public Language build() {
        return lang;
    }
}
