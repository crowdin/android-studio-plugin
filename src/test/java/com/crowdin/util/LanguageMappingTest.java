package com.crowdin.util;

import com.crowdin.api.model.LanguageBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LanguageMappingTest {

    private static final String nonExistingLanguageId = "non-existing-lang-id";
    private static final String nonExistingPlaceHolder = "non-existing-place-holder";
    private static final String existingLanguageId = LanguageBuilder.UKR.build().getId();
    private static final String existingPlaceHolder = PlaceholderUtil.PLACEHOLDER_LANGUAGE_ID_NAME;
    private static final String defaultLanguageId = LanguageBuilder.ENG.build().getId();
    private static final Map<String, Map<String, String>> testServerMappings = new HashMap<String, Map<String, String>>() {{
        put(existingLanguageId, new HashMap<String, String>(){{
            put(existingPlaceHolder, existingLanguageId);
        }});
    }};



    @ParameterizedTest
    @MethodSource
    public void testContainsValue(Map<String, Map<String, String>> serverMappings, String langCode, String placeHolder, boolean expected) {
        LanguageMapping languageMapping = LanguageMapping.fromServerLanguageMapping(serverMappings);
        boolean result = languageMapping.containsValue(langCode, placeHolder);

        assertEquals(expected, result);
    }

    public static Stream<Arguments> testContainsValue() {
        return Stream.of(
                arguments(null, existingLanguageId, existingPlaceHolder, false),
                arguments(testServerMappings, existingLanguageId, nonExistingPlaceHolder, false),
                arguments(testServerMappings, nonExistingLanguageId, existingPlaceHolder, false),
                arguments(testServerMappings, nonExistingLanguageId, nonExistingPlaceHolder, false),
                arguments(testServerMappings, existingLanguageId, existingPlaceHolder, true)
                );
    }

    @ParameterizedTest
    @MethodSource
    public void testGetValue(Map<String, Map<String, String>> serverMappings, String langCode, String placeHolder, String expected) {
        LanguageMapping languageMapping = LanguageMapping.fromServerLanguageMapping(serverMappings);
        String result = languageMapping.getValue(langCode, placeHolder);

        assertEquals(expected, result);
    }

    public static Stream<Arguments> testGetValue() {
        return Stream.of(
                arguments(null, existingLanguageId, existingPlaceHolder, null),
                arguments(testServerMappings, existingLanguageId, nonExistingPlaceHolder, null),
                arguments(testServerMappings, nonExistingLanguageId, existingPlaceHolder, null),
                arguments(testServerMappings, nonExistingLanguageId, nonExistingPlaceHolder, null),
                arguments(testServerMappings, existingLanguageId, existingPlaceHolder, existingLanguageId)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testGetValueOrDefault(Map<String, Map<String, String>> serverMappings, String langCode, String placeHolder, String defaultValue, String expected) {
        LanguageMapping languageMapping = LanguageMapping.fromServerLanguageMapping(serverMappings);
        String result = languageMapping.getValueOrDefault(langCode, placeHolder, defaultValue);

        assertEquals(expected, result);
    }

    public static Stream<Arguments> testGetValueOrDefault() {
        return Stream.of(
                arguments(null, existingLanguageId, existingPlaceHolder, defaultLanguageId, defaultLanguageId),
                arguments(testServerMappings, existingLanguageId, nonExistingPlaceHolder, defaultLanguageId, defaultLanguageId),
                arguments(testServerMappings, nonExistingLanguageId, existingPlaceHolder, defaultLanguageId, defaultLanguageId),
                arguments(testServerMappings, nonExistingLanguageId, nonExistingPlaceHolder, defaultLanguageId, defaultLanguageId),
                arguments(testServerMappings, existingLanguageId, existingPlaceHolder, defaultLanguageId, existingLanguageId)
        );
    }
}