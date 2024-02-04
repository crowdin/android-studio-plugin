package com.crowdin.client.config;

import com.crowdin.client.FileBeanBuilder;
import com.crowdin.settings.CrowdingSettingsState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.crowdin.client.config.CrowdinPropertiesLoader.isBaseUrlValid;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CrowdinPropertiesLoaderTest {

    private static final Yaml YAML = new Yaml();

    private final CrowdingSettingsState settingsState = new CrowdingSettingsState() {
        @Override
        public String getApiToken() {
            return "test";
        }
    };

    {
        settingsState.projectId = "123";
    }

    @Test
    public void isBaseUrlValidTest() {
        Assertions.assertTrue(isBaseUrlValid("https://myorganization.crowdin.com/"));
        Assertions.assertTrue(isBaseUrlValid("https://crowdin.com"));
        Assertions.assertTrue(isBaseUrlValid("http://test.dev.crowdin.com"));
        Assertions.assertTrue(isBaseUrlValid("http://my-organization.test.dev.crowdin.com"));
        Assertions.assertTrue(isBaseUrlValid("https://ti-it.crowdin.com"));
        Assertions.assertFalse(isBaseUrlValid("http://my-organization.testdev.crowdin.com"));
        Assertions.assertFalse(isBaseUrlValid("http://crowdin.com"));
        Assertions.assertFalse(isBaseUrlValid("http://myorganization.crowdin.com"));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetSources(Map<String, Object> properties, List<FileBean> expected) {
        CrowdinConfig result = CrowdinPropertiesLoader.load(properties, settingsState);
        Assertions.assertEquals(expected, result.getFiles(), "Properties: " + properties);
    }

    public static Stream<Arguments> testGetSources() {
        return Stream.of(
                arguments(
                        getProperties("file1.yml"),
                        new ArrayList<FileBean>() {{
                            add(FileBeanBuilder.fileBean("values2/*.xml", "/values2-%android_code%/%original_file_name%").build());
                        }}),
                arguments(
                        getProperties("file2.yml"),
                        new ArrayList<FileBean>() {{
                            add(FileBeanBuilder.fileBean("values/*.xml", "/values-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("another/path/*.xml", "/another/path-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("values2/*.xml", "/values2-%android_code%/%original_file_name%").build());
                        }})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testThrowRunTimeException(Map<String, Object> properties) {
        assertThrows(RuntimeException.class, () -> CrowdinPropertiesLoader.load(properties, settingsState));
    }

    public static Stream<Arguments> testThrowRunTimeException() {
        return Stream.of(
                arguments((Object) null)
        );
    }

    public static Map<String, Object> getProperties(String file) {
        try {
            InputStream inputStream = CrowdinPropertiesLoaderTest.class.getClassLoader().getResourceAsStream(file);
            return YAML.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }
}
