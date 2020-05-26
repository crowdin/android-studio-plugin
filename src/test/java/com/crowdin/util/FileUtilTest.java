package com.crowdin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class FileUtilTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @BeforeEach
    public void setup() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        super.tearDown();
    }

    @ParameterizedTest
    @MethodSource
    public void testGetSourceFilesRec(List<String> filesNeeded, String sourcePattern, List<String> expected) {
        VirtualFile root = myFixture.copyFileToProject(filesNeeded.get(0));
        while (root.getParent() != null) {
            root = root.getParent();
        }
        for (int i = 1; i < filesNeeded.size(); i++) {
            myFixture.copyFileToProject(filesNeeded.get(i));
        }
        List<VirtualFile> files = FileUtil.getSourceFilesRec(root, sourcePattern);
        List<String> result = files.stream()
            .map(VirtualFile::getPath)
            .collect(Collectors.toList());
        assertEquals("Source pattern: " + sourcePattern, expected, result);
    }

    public static Stream<Arguments> testGetSourceFilesRec() {
        String root = "/src/";
        return Stream.of(
            arguments(
                Arrays.asList("values/strings.xml"),
                "values/strings.xml",
                Arrays.asList(root + "values/strings.xml")),
            arguments(
                Arrays.asList("values/strings.xml", "values/strings2.xml"),
                "values/*",
                Arrays.asList(root + "values/strings.xml", root + "values/strings2.xml")),
            arguments(
                Arrays.asList("values/strings.xml", "values/strings2.xml"),
                "strings*.xml",
                Arrays.asList(root + "values/strings.xml", root + "values/strings2.xml")),
            arguments(
                Arrays.asList("values/strings.xml", "values/strings2.xml"),
                "values/*2.xml",
                Arrays.asList(root + "values/strings2.xml"))
        );
    }
}
