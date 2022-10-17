package com.crowdin.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
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
        while (root.getParent().getParent() != null) {
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
                new ArrayList<>()),
            arguments(
                Arrays.asList("values/strings.xml", "values/strings2.xml"),
                "values/*2.xml",
                Arrays.asList(root + "values/strings2.xml")),
            arguments(
                Arrays.asList("values/strings.xml", "values/second/values/strings.xml"),
                "values/strings.xml",
                Arrays.asList(root + "values/strings.xml"))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testFindRelativePath(String baseDirPath, String filePath, String expected) {
        VirtualFile baseDir = myFixture.copyFileToProject(baseDirPath);
        VirtualFile file = myFixture.copyFileToProject(filePath);

        String result = FileUtil.findRelativePath(baseDir, file);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testFindRelativePath () {
        String root = "src/";
        return Stream.of(
                arguments(
                        "values/strings.xml",
                        "values/second/values/strings.xml",
                        root + "values/second/values/strings.xml"),
                arguments(
                        "values/strings.xml",
                        "values/strings2.xml",
                        root + "values/strings2.xml"));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetBaseDir(String baseDirPath, String filePath) {
        VirtualFile file = myFixture.copyFileToProject(filePath);
        VirtualFile expectedFile = file.getParent().getParent();

        VirtualFile result = FileUtil.getBaseDir(file, baseDirPath);
        assertEquals(expectedFile, result);
    }

    public static Stream<Arguments> testGetBaseDir() {
        String root = "src/";
        return Stream.of(
                arguments(
                        "values/strings.xml",
                        "values/second/values/strings.xml"),
                arguments(
                        "values/strings.xml",
                        "values/strings2.xml"));
    }

    @ParameterizedTest
    @MethodSource
    public void testWalkDir(String filePath, List<File> expected) {
        Path path = Paths.get(filePath);

        List<File> result = FileUtil.walkDir(path);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testWalkDir() {
        String root = "src/";
        File file = new File("src/test/testData/values/second/values/strings.xml");
        File file2 = new File("src/test/testData/values/strings.xml");
        File file3 = new File("src/test/testData/values/strings2.xml");
        return Stream.of(
                arguments(
                        root+"test/testData/values",
                        Arrays.asList(file, file2, file3)));
    }

    @ParameterizedTest
    @MethodSource
    public void testWalkDirWithException(String filePath) {
        Path path = Paths.get(filePath);
        assertThrows(RuntimeException.class,() -> FileUtil.walkDir(path));
    }

    public static Stream<Arguments> testWalkDirWithException() {
        return Stream.of(arguments("/invalidPath/"));
    }

    @ParameterizedTest
    @MethodSource
    public void testDownloadFile(Object requestor, String filePath, URL url) {
        VirtualFile file = myFixture.copyFileToProject(filePath);
        FileUtil.downloadFile(requestor, file, url);
        assertTrue(file.getLength()>0);
    }

    public static Stream<Arguments> testDownloadFile() throws MalformedURLException {
        return Stream.of(arguments(new Object(), "values/strings.xml", new URL("http://www.example.com")));
    }

    @ParameterizedTest
    @MethodSource
    public void testFilePathRegex(String filePathPattern, boolean preserveHierarchy, boolean expected) {
        Predicate<String> result = FileUtil.filePathRegex(filePathPattern, preserveHierarchy);
        assertEquals(expected, result.test(filePathPattern));
    }

    public static Stream<Arguments> testFilePathRegex() {
        String root = "src/";
        File file = new File("src/test/testData/values/second/values/strings.xml");
        File file2 = new File("src/test/testData/values/strings.xml");
        File file3 = new File("src/test/testData/values/strings2.xml");
        return Stream.of(
                arguments(
                        root+"test/testData/values/strings.xml",
                        true, true),
        arguments(
                root+"test/testData/values/strings.xml",
                false, false));
    }

    @ParameterizedTest
    @MethodSource
    public void testDownloadTempFile(String filePath) throws Exception {
        File file = new File(filePath);
        File result = FileUtil.downloadTempFile(new FileInputStream(file));
        assertTrue(result.isFile());
    }

    public static Stream<Arguments> testDownloadTempFile() {
        return Stream.of(arguments("src/test/testData/values/second/values/strings.xml"));
    }

    @ParameterizedTest
    @MethodSource
    public void testCreateIfNeededFilePath(Object requestor, String filePath, String expected) throws IOException {
        VirtualFile file = myFixture.copyFileToProject(filePath).getParent().getParent();
        VirtualFile result = FileUtil.createIfNeededFilePath(requestor, file, filePath);
        assertEquals(expected, result.getCanonicalPath());
    }

    public static Stream<Arguments> testCreateIfNeededFilePath() throws MalformedURLException {
        return Stream.of(arguments(new Object(), "values/strings.xml", "/src/values/strings.xml"));
    }

    @ParameterizedTest
    @MethodSource
    public void testUnixPath(String filePath, String expected) {
        String result = FileUtil.unixPath(filePath);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testUnixPath() {
        return Stream.of(arguments("values\\\\/second\\\\/values\\\\/strings.xml", "values/second/values/strings.xml"));
    }
    @ParameterizedTest
    @MethodSource
    public void testJoinPaths(String expected, String... filePath) {
        String result = FileUtil.joinPaths(filePath);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testJoinPaths() {
        return Stream.of(
                arguments("values\\second\\values\\strings.xml",
                        new String[]{"values\\\\/", "second\\\\/", "values\\\\/", "strings.xml"}));
    }

    @ParameterizedTest
    @MethodSource
    public void testSepAtStart(String filePath, String expected) {
        String result = FileUtil.sepAtStart(filePath);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testSepAtStart() {
        return Stream.of(
                arguments("values/second/values/strings.xml", "\\values/second/values/strings.xml"));
    }

    @ParameterizedTest
    @MethodSource
    public void testSepAtEnd(String filePath, String expected) {
        String result = FileUtil.sepAtEnd(filePath);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testSepAtEnd() {
        return Stream.of(
                arguments("values/second/values/strings.xml", "values/second/values/strings.xml\\"));
    }
}
