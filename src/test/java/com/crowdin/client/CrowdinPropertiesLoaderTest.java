package com.crowdin.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

import static com.crowdin.client.CrowdinPropertiesLoader.isBaseUrlValid;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CrowdinPropertiesLoaderTest {

    @Test
    public void isBaseUrlValidTest() {
        assertTrue(isBaseUrlValid("https://myorganization.crowdin.com/"));
        assertTrue(isBaseUrlValid("https://crowdin.com"));
        assertTrue(isBaseUrlValid("http://test.dev.crowdin.com"));
        assertTrue(isBaseUrlValid("http://my-organization.test.dev.crowdin.com"));
        assertTrue(isBaseUrlValid("https://ti-it.crowdin.com"));
        assertFalse(isBaseUrlValid("http://my-organization.testdev.crowdin.com"));
        assertFalse(isBaseUrlValid("http://crowdin.com"));
        assertFalse(isBaseUrlValid("http://myorganization.crowdin.com"));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetSources(Properties properties, List<FileBean> expected) {
        CrowdinProperties result = CrowdinPropertiesLoader.load(properties);
        assertEquals("Properties: " + properties, expected, result.getFiles());
    }

    public static Stream<Arguments> testGetSources() {
        ClassLoader classLoader = CrowdinPropertiesLoaderTest.class.getClassLoader();
        return Stream.of(
                arguments(
                        getProperties(new java.io.File(classLoader.getResource("properties/sources/one_source.properties").getFile())),
                        new ArrayList<FileBean>() {{
                            add(FileBeanBuilder.fileBean("**/values/strings.xml", "/values-%android_code%/%original_file_name%").build());
                        }}),
                arguments(
                        getProperties(new java.io.File(classLoader.getResource("properties/sources/sources_w_translations.properties").getFile())),
                        new ArrayList<FileBean>() {{
                            add(FileBeanBuilder.fileBean("values/*.xml", "/values-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("another/path/*.xml", "/another/path-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("values2/*.xml", "/values2-%android_code%/%original_file_name%").build());
                        }}),
                arguments(
                        getProperties(new java.io.File(classLoader.getResource("properties/sources/combined_sources.properties").getFile())),
                        new ArrayList<FileBean>() {{
                            add(FileBeanBuilder.fileBean("**/values/strings.xml", "/values-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("**/values/strings1.xml", "/values-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("**/values/strings2.xml", "/values-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("values/*.xml", "/values-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("another/path/*.xml", "/another/path-%android_code%/%original_file_name%").build());
                            add(FileBeanBuilder.fileBean("values2/*.xml", "/values2-%android_code%/%original_file_name%").build());
                        }})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testThrowRunTimeException(Properties properties) {
        assertThrows(RuntimeException.class, () -> CrowdinPropertiesLoader.load(properties));
    }

    public static Stream<Arguments> testThrowRunTimeException() {
        return Stream.of(
                arguments((Object) null)
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
