package com.crowdin.util;

import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PropertyUtilTest {

    @ParameterizedTest
    @MethodSource
    public void testGetSources(Properties properties, Map<String, String> expected) {
        CrowdinProperties result = CrowdinPropertiesLoader.load(properties);
        assertEquals("Properties: " + properties, expected, result.getSourcesWithPatterns());
    }

    public static Stream<Arguments> testGetSources() {
        ClassLoader classLoader = PropertyUtilTest.class.getClassLoader();
        return Stream.of(
            arguments(
                getProperties(new java.io.File(classLoader.getResource("properties/sources/one_source.properties").getFile())),
                new HashMap<String, String>() {{
                    put("**/values/strings.xml", "/values-%android_code%/%original_file_name%");
                }}),
            arguments(
                    getProperties(new java.io.File(classLoader.getResource("properties/sources/list_sources.properties").getFile())),
                new HashMap<String, String>() {{
                    put("**/values/strings.xml", "/values-%android_code%/%original_file_name%");
                    put("**/values/strings1.xml", "/values-%android_code%/%original_file_name%");
                    put("**/values/strings2.xml", "/values-%android_code%/%original_file_name%");
                }}),
            arguments(
                    getProperties(new java.io.File(classLoader.getResource("properties/sources/sources_w_translations.properties").getFile())),
                new HashMap<String, String>() {{
                    put("values/*.xml", "/values-%android_code%/%original_file_name%");
                    put("another/path/*.xml", "/another/path-%android_code%/%original_file_name%");
                    put("values2/*.xml", "/values2-%android_code%/%original_file_name%");
                }}),
            arguments(
                    getProperties(new java.io.File(classLoader.getResource("properties/sources/combined_sources.properties").getFile())),
                new HashMap<String, String>() {{
                    put("**/values/strings.xml", "/values-%android_code%/%original_file_name%");
                    put("**/values/strings1.xml", "/values-%android_code%/%original_file_name%");
                    put("**/values/strings2.xml", "/values-%android_code%/%original_file_name%");
                    put("values/*.xml", "/values-%android_code%/%original_file_name%");
                    put("another/path/*.xml", "/another/path-%android_code%/%original_file_name%");
                    put("values2/*.xml", "/values2-%android_code%/%original_file_name%");
                }})
        );
    }

    public static Properties getProperties(java.io.File file) {
        Properties properties = new Properties();
        try {
            InputStream in = new FileInputStream(file.getCanonicalPath());
            properties.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }
}
