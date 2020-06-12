package com.crowdin.util;

import com.crowdin.api.model.DirectoryBuilder;
import com.crowdin.api.model.FileBuilder;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
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
}
