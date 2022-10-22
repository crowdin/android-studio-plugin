package com.crowdin.client;

import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.util.LanguageMapping;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class CrowdinProjectCacheProviderTest {

    @ParameterizedTest
    @MethodSource
    public void testGetFileInfos(final Map<Branch, Map<String, ? extends FileInfo>> fileInfo,
                                 final Branch branch, final Map<String, FileInfo> expected) {
        CrowdinProjectCacheProvider.CrowdinProjectCache cache = new CrowdinProjectCacheProvider.CrowdinProjectCache();
        cache.setFileInfos(fileInfo);
        final Map<String, FileInfo> result = cache.getFileInfos(branch);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> testGetFileInfos() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setProjectId(9875L);
        Branch branch = new Branch();
        branch.setProjectId(9875L);
        Branch branch1 = new Branch();
        branch1.setProjectId(9786L);
        Map<String, FileInfo> map = new HashMap<>();
        map.put("fileinfo", fileInfo);
        Map<String, FileInfo> map2 = new HashMap<>();
        Map<Branch, Map<String,FileInfo>> map1 = new HashMap<>();
        map1.put(branch, map);
        return Stream.of(arguments(map1, branch, map), arguments(map1, branch1, map2));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetFiles(final Map<Branch, Map<String, ? extends FileInfo>> fileInfo,
                                 final Branch branch, final Map<String, File> expected) {
        CrowdinProjectCacheProvider.CrowdinProjectCache cache = new CrowdinProjectCacheProvider.CrowdinProjectCache();
        cache.setFileInfos(fileInfo);
        cache.setManagerAccess(true);
        final Map<String, File> result = cache.getFiles(branch);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> testGetFiles() {
        File file = new File();
        file.setProjectId(9875L);
        Branch branch = new Branch();
        branch.setProjectId(9875L);
        Branch branch1 = new Branch();
        branch1.setProjectId(9786L);
        Map<String, File> map = new HashMap<>();
        map.put("fileinfo", file);
        Map<String, File> map2 = new HashMap<>();
        Map<Branch, Map<String,File>> map1 = new HashMap<>();
        map1.put(branch, map);
        return Stream.of(arguments(map1, branch, map), arguments(map1, branch1, map2));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetLanguageMapping(final LanguageMapping mapping, final LanguageMapping expected) {
        CrowdinProjectCacheProvider.CrowdinProjectCache cache = new CrowdinProjectCacheProvider.CrowdinProjectCache();
        cache.setLanguageMapping(mapping);
        cache.setManagerAccess(true);
        final LanguageMapping result = cache.getLanguageMapping();
        assertEquals(expected, result);
    }

    private static Stream<Arguments> testGetLanguageMapping() {
        final LanguageMapping languageMapping = LanguageMapping.fromServerLanguageMapping(null);
        return Stream.of(arguments(languageMapping, languageMapping));
    }

    @ParameterizedTest
    @MethodSource
    public void testRunTimeException(final LanguageMapping mapping) {
        CrowdinProjectCacheProvider.CrowdinProjectCache cache = new CrowdinProjectCacheProvider.CrowdinProjectCache();
        cache.setManagerAccess(false);
        cache.setLanguageMapping(mapping);
        assertThrows(RuntimeException.class, cache::getLanguageMapping);
    }

    private static Stream<Arguments> testRunTimeException() {
        final LanguageMapping languageMapping = LanguageMapping.fromServerLanguageMapping(null);
        return Stream.of(arguments(languageMapping));
    }

    @ParameterizedTest
    @MethodSource
    public void testAddOutdatedBranch(final String branchName) {
        assertDoesNotThrow(() ->CrowdinProjectCacheProvider.outdateBranch(branchName));
    }

    private static Stream<Arguments> testAddOutdatedBranch() {
        return Stream.of(arguments("branchName"));
    }
}
