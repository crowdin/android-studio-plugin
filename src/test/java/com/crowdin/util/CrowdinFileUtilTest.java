package com.crowdin.util;

import com.crowdin.api.model.DirectoryBuilder;
import com.crowdin.api.model.FileBuilder;
import com.crowdin.api.model.LanguageBuilder;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcefiles.model.GeneralFileExportOptions;
import com.crowdin.client.sourcefiles.model.PropertyFileExportOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CrowdinFileUtilTest {

    private static final Long PROJECT_ID = 10L;
    private static final String sep = java.io.File.separator;

    @ParameterizedTest
    @MethodSource
    public void testBuildFilePaths(List<File> files, Map<Long, Directory> dirs, Map<String, File> expected) {
        Map<String, File> result = CrowdinFileUtil.buildFilePaths(files, dirs);
        assertEquals("dirs: " + dirs + ", files: " + files, expected, result);
    }

    public static Stream<Arguments> testBuildFilePaths() {
        Directory dir_201L = DirectoryBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("values", 201L, null, 301L).build();
        File file_101L_201L = FileBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("strings.xml", "xml", 101L, 201L, 301L).build();
        File file_102L_null = FileBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("strings2.xml", "xml", 102L, null, 301L).build();
        return Stream.of(
            arguments(new ArrayList<File>() {{
                    add(file_101L_201L);
                    add(file_102L_null);
                }},new HashMap<Long, Directory>() {{
                    put(201L, dir_201L);
                }},  new HashMap<String, File>() {{
                    put(sep + "values" + sep + "strings.xml", file_101L_201L);
                    put(sep + "strings2.xml", file_102L_null);
                }})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIsValidBranchName(String branchName, boolean expected) {
        boolean result = CrowdinFileUtil.isValidBranchName(branchName);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testIsValidBranchName() {
        return Stream.of(
            arguments("master", true),
            arguments(null, true),
            arguments("test<42>", false),
            arguments("\\4\\2\\", false),
            arguments("", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testBuildDirPaths(Map<Long, Directory> dirs, Map<String, Directory> expected) {
        Map<String, Directory> result = CrowdinFileUtil.buildDirPaths(dirs);
        assertEquals("dirs: " + dirs, expected, result);
    }

    public static Stream<Arguments> testBuildDirPaths() {
        Directory dir_201L = DirectoryBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("values", 201L, 202L, 301L).build();
        Directory dir_202L = DirectoryBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("values", 202L, null, 301L).build();

        return Stream.of(
                arguments(new HashMap<Long, Directory>() {{
                    put(201L, dir_201L);
                    put(202L, dir_202L);
                }}, new HashMap<String, Directory>() {{
                    put(sep + "values", dir_202L);
                    put(sep + "values" + sep + "values", dir_201L);
                }})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testRevDirPaths(Map<String, Directory> dirs, Map<Long, String> expected) {
        Map<Long, String> result = CrowdinFileUtil.revDirPaths(dirs);
        assertEquals("dirs: " + dirs, expected, result);
    }

    public static Stream<Arguments> testRevDirPaths() {
        Directory dir_201L = DirectoryBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("values", 201L, null, 301L).build();
        Directory dir_202L = DirectoryBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("values", 202L, null, 301L).build();

        return Stream.of(
                arguments(new HashMap<String, Directory>() {{
                    put("201", dir_201L);
                    put("202", dir_202L);
                }}, new HashMap<Long, String>() {{
                    put(201L, "201");
                    put(202L, "202");
                }})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testBuildAllProjectTranslationsWithSources(List<File> files, Map<Long, String> dirPaths, List<Language> languages, LanguageMapping languageMapping, Map<String, String> expected) {
        Map<String, String> result = CrowdinFileUtil.buildAllProjectTranslationsWithSources(files, dirPaths, languages, languageMapping);
        assertEquals("dirPaths: " + dirPaths + ", files: " + files, expected, result);
    }

    public static Stream<Arguments> testBuildAllProjectTranslationsWithSources() {
        GeneralFileExportOptions generalFileExportOptions = new GeneralFileExportOptions();
        generalFileExportOptions.setExportPattern("pattern");
        PropertyFileExportOptions propertyFileExportOptions = new PropertyFileExportOptions();
        propertyFileExportOptions.setExportPattern("pattern2");
        File file_101L_201L = FileBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("strings.xml", "xml", 101L, 201L, 301L).build();
        File file_102L_null = FileBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("strings2.xml", "xml", 102L, null, 301L).setExportOptions(generalFileExportOptions).build();
        File file_103L_null = FileBuilder.standard().setProjectId(PROJECT_ID).setIdentifiers("strings3.xml", "xml", 103L, null, 301L).setExportOptions(propertyFileExportOptions).build();
        Language lang = LanguageBuilder.ENG.build();
        LanguageMapping languageMapping = LanguageMapping.fromServerLanguageMapping(null);
        return Stream.of(
                arguments(new ArrayList<File>() {{
                    add(file_101L_201L);
                    add(file_102L_null);
                    add(file_103L_null);
                }}, new HashMap<Long, String>() {{
                    put(201L, "values");
                }}, new ArrayList<Language>() {{
                    add(lang);
                }}, languageMapping, new HashMap<String, String>() {{
                    put("values" + sep + "strings.xml", "values" + sep + "strings.xml");
                    put("pattern", sep + "strings2.xml");
                    put("pattern2", sep + "strings3.xml");
                }})
        );
    }
}
