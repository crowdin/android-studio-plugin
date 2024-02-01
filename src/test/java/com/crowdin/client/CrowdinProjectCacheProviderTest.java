package com.crowdin.client;

import com.crowdin.client.projectsgroups.model.Project;
import com.crowdin.client.projectsgroups.model.ProjectSettings;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.service.CrowdinProjectCacheProvider.CrowdinProjectCache;
import com.crowdin.util.LanguageMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.crowdin.client.MockCrowdin.STANDARD_BRANCH_1;
import static com.crowdin.client.MockCrowdin.STANDARD_BRANCH_2;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class CrowdinProjectCacheProviderTest {

    private CrowdinProjectCacheProvider crowdinProjectCacheProvider;

    @BeforeEach
    void setUp() {
        crowdinProjectCacheProvider = new CrowdinProjectCacheProvider();
    }

    @ParameterizedTest
    @MethodSource
    public void testGetFileInfos(final Map<Branch, Map<String, ? extends FileInfo>> fileInfo,
                                 final Branch branch, final Map<String, FileInfo> expected) {
        CrowdinProjectCache cache = new CrowdinProjectCache();
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
        Map<Branch, Map<String, FileInfo>> map1 = new HashMap<>();
        map1.put(branch, map);
        return Stream.of(arguments(map1, branch, map), arguments(map1, branch1, map2));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetFiles(final Map<Branch, Map<String, ? extends FileInfo>> fileInfo,
                             final Branch branch, final Map<String, File> expected) {
        CrowdinProjectCache cache = new CrowdinProjectCache();
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
        Map<Branch, Map<String, File>> map1 = new HashMap<>();
        map1.put(branch, map);
        return Stream.of(arguments(map1, branch, map), arguments(map1, branch1, map2));
    }

    @ParameterizedTest
    @MethodSource
    public void testGetLanguageMapping(final LanguageMapping mapping, final LanguageMapping expected) {
        CrowdinProjectCache cache = new CrowdinProjectCache();
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
        CrowdinProjectCache cache = new CrowdinProjectCache();
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
        assertDoesNotThrow(() -> crowdinProjectCacheProvider.outdateBranch(branchName));
    }

    private static Stream<Arguments> testAddOutdatedBranch() {
        return Stream.of(arguments("branchName"));
    }

    //getInstance()

    @Test
    void testSetsValuesForNotYetConfiguredCacheProperties() {
        CrowdinClient crowdin = new MockCrowdin(1L);
        //Using a different branch name than what's in MockCrowdin, so that fileinfos doesn't get updated
        CrowdinProjectCache cache = crowdinProjectCacheProvider.getInstance(crowdin, "branchname", false);

        assertEquals(crowdin.getProject(), cache.getProject());
        assertFalse(cache.isManagerAccess());
        assertEquals(crowdin.getStrings(), cache.getStrings());
        assertEquals(crowdin.getSupportedLanguages(), cache.getSupportedLanguages());
        assertEquals(crowdin.extractProjectLanguages(crowdin.getProject()), cache.getProjectLanguages());
        assertEquals(crowdin.getBranches(), cache.getBranches());

        assertNotNull(cache.getFileInfos());
        assertEquals(0, cache.getFileInfos().size());

        assertNotNull(cache.getDirs());
        assertEquals(0, cache.getDirs().size());
    }

    @Test
    void testDoesntSetConfigurationForExistingValues() {
        //Set the initial state of the underlying project cache. Using empty collections to have simpler test data.
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        Project project = new Project();
        cache.setProject(project);
        cache.setManagerAccess(false);
        cache.setStrings(new ArrayList<>());
        cache.setSupportedLanguages(new ArrayList<>());
        cache.setProjectLanguages(new ArrayList<>());
        cache.setBranches(new HashMap<>());

        //File infos and dirs are initialized to non-empty collections here,
        // because they are initialized as empty collections in getInstance(),
        // so we can properly see a failure if that occurs.
        Map<Branch, Map<String, ? extends FileInfo>> fileInfos = createFileInfos(STANDARD_BRANCH_1, new HashMap<>());
        cache.setFileInfos(fileInfos);

        Map<Branch, Map<String, Directory>> dirs = createDirs(STANDARD_BRANCH_1, new HashMap<>());
        cache.setDirs(dirs);

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch1", false);

        //Validating 0 collection sizes is feasible because we presume
        // that the used MockCrowdin instance returns non-empty collections
        assertSame(project, cache.getProject());
        assertFalse(cache.isManagerAccess());
        assertEquals(0, cache.getStrings().size());
        assertEquals(0, cache.getSupportedLanguages().size());
        assertEquals(0, cache.getProjectLanguages().size());
        assertEquals(0, cache.getBranches().size());
        assertEquals(fileInfos, cache.getFileInfos());
        assertEquals(dirs, cache.getDirs());
    }

    @Test
    void testSetsLanguageMappingForManagerAccess() {
        CrowdinClient crowdin = new MockCrowdin(2L);
        CrowdinProjectCache cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch1", false);

        assertTrue(cache.isManagerAccess());
        assertTrue(cache.getLanguageMapping().containsValue("hu", "placeholder"));
    }

    @Test
    void testSetsBranchesWhenOutdated() {
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);
        cache.setBranches(new HashMap<>());
        crowdinProjectCacheProvider.outdateBranch("branch1");

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch1", false);

        assertEquals(crowdin.getBranches(), cache.getBranches());
    }

    @Test
    void testDoesntSetConfigurationForNoUpdate() {
        //Set the initial state of the underlying project cache. Using empty collections to have simpler test data.
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        //Using ProjectSettings because that would set manager access to true,
        // thus we can validate if it remains false
        Project project = new ProjectSettings();
        cache.setProject(project);
        cache.setProjectLanguages(new ArrayList<>());
        cache.setBranches(new HashMap<>());

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch1", false);

        //Test that no configuration was overwritten with data from the mock Crowdin client
        assertSame(project, cache.getProject());
        assertFalse(cache.isManagerAccess());
        assertEquals(0, cache.getProjectLanguages().size());
        assertEquals(0, cache.getBranches().size());
    }

    @Test
    void testSetsConfigurationForUpdate() {
        //Set the initial state of the underlying project cache. Using empty collections to have simpler test data.
        CrowdinProjectCache cache = new CrowdinProjectCache();
        Project prj = new Project();
        prj.setId(1L);
        cache.setProject(prj);
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        //Using ProjectSettings because that would set manager access to true,
        // thus we can validate if it remains false.
        Project project = new ProjectSettings();
        cache.setProject(project);
        cache.setProjectLanguages(new ArrayList<>());
        cache.setBranches(new HashMap<>());

        CrowdinClient crowdin = new MockCrowdin(prj.getId());
        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch1", true);

        //Test that no configuration was overwritten with data from the mock Crowdin client
        assertSame(crowdin.getProject(), cache.getProject());
        assertFalse(cache.isManagerAccess()); //false because using project id 1L
        assertEquals(crowdin.extractProjectLanguages(crowdin.getProject()), cache.getProjectLanguages());
        assertEquals(crowdin.getBranches(), cache.getBranches());
    }

    //getInstance() - update file infos and dirs for branch

    @Test
    void testSavesBranchWhenFileInfosDoesntContainBranch() {
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache.setBranches(crowdin.getBranches());
        //FileInfos doesn't contain branch2
        cache.setFileInfos(createFileInfos(STANDARD_BRANCH_1, new HashMap<>()));

        crowdinProjectCacheProvider.outdateBranch("branch2");

        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch2", false);

        assertTrue(cache.getFileInfos().containsKey(STANDARD_BRANCH_2));
        assertTrue(cache.getDirs().containsKey(STANDARD_BRANCH_2));
        assertFalse(crowdinProjectCacheProvider.getOutdatedBranches().contains("branch2"));
    }

    @Test
    void testSavesBranchWhenDirsDoesntContainBranch() {
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache.setBranches(crowdin.getBranches());
        //FileInfos contains branch2, Dirs doesn't contain branch2
        HashMap<String, FileInfo> fileInfoValue = new HashMap<>();
        cache.setFileInfos(createFileInfos(STANDARD_BRANCH_2, fileInfoValue));
        cache.setDirs(createDirs(STANDARD_BRANCH_1, new HashMap<>()));

        crowdinProjectCacheProvider.outdateBranch("branch2");

        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch2", false);

        //Validate if fileinfos branch entry value is updated
        assertNotSame(fileInfoValue, cache.getFileInfos().get(STANDARD_BRANCH_2));
        assertTrue(cache.getDirs().containsKey(STANDARD_BRANCH_2));
        assertFalse(crowdinProjectCacheProvider.getOutdatedBranches().contains("branch2"));
    }

    @Test
    void testSavesBranchWhenBranchIsOutdated() {
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache.setBranches(crowdin.getBranches());
        //FileInfos and dirs both contain branch2
        HashMap<String, FileInfo> fileInfoValue = new HashMap<>();
        cache.setFileInfos(createFileInfos(STANDARD_BRANCH_2, fileInfoValue));
        HashMap<String, Directory> dirsValue = new HashMap<>();
        cache.setDirs(createDirs(STANDARD_BRANCH_2, dirsValue));

        crowdinProjectCacheProvider.outdateBranch("branch2");

        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch2", false);

        //Validate if fileinfos and dirs branch entry values are updated
        assertNotSame(fileInfoValue, cache.getFileInfos().get(STANDARD_BRANCH_2));
        assertNotSame(dirsValue, cache.getDirs().get(STANDARD_BRANCH_2));
        assertFalse(crowdinProjectCacheProvider.getOutdatedBranches().contains("branch2"));
    }

    @Test
    void testSavesBranchWhenMarkedForUpdate() {
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache.setBranches(crowdin.getBranches());
        //FileInfos and dirs both contain branch2
        HashMap<String, FileInfo> fileInfoValue = new HashMap<>();
        cache.setFileInfos(createFileInfos(STANDARD_BRANCH_2, fileInfoValue));
        HashMap<String, Directory> dirsValue = new HashMap<>();
        cache.setDirs(createDirs(STANDARD_BRANCH_2, dirsValue));

        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch2", true);

        //Validate if fileinfos and dirs branch entry values are updated
        assertNotSame(fileInfoValue, cache.getFileInfos().get(STANDARD_BRANCH_2));
        assertNotSame(dirsValue, cache.getDirs().get(STANDARD_BRANCH_2));
        assertFalse(crowdinProjectCacheProvider.getOutdatedBranches().contains("branch2"));
    }

    @Test
    void testDoesntSaveBranch() {
        CrowdinProjectCache cache = new CrowdinProjectCache();
        crowdinProjectCacheProvider.setCrowdinProjectCache(cache);

        CrowdinClient crowdin = new MockCrowdin(1L);
        cache.setBranches(crowdin.getBranches());
        //FileInfos and dirs both contain branch2
        HashMap<String, FileInfo> fileInfoValue = new HashMap<>();
        cache.setFileInfos(createFileInfos(STANDARD_BRANCH_2, fileInfoValue));
        HashMap<String, Directory> dirsValue = new HashMap<>();
        cache.setDirs(createDirs(STANDARD_BRANCH_2, dirsValue));

        cache = crowdinProjectCacheProvider.getInstance(crowdin, "branch2", false);

        //Validate if fileinfos and dirs branch entry values are updated
        assertSame(fileInfoValue, cache.getFileInfos().get(STANDARD_BRANCH_2));
        assertSame(dirsValue, cache.getDirs().get(STANDARD_BRANCH_2));
    }

    //Helper methods

    private Map<Branch, Map<String, ? extends FileInfo>> createFileInfos(Branch key, Map<String, ? extends FileInfo> value) {
        Map<Branch, Map<String, ? extends FileInfo>> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private Map<Branch, Map<String, Directory>> createDirs(Branch key, Map<String, Directory> value) {
        Map<Branch, Map<String, Directory>> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
