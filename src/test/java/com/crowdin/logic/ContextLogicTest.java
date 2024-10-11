package com.crowdin.logic;

import com.crowdin.client.config.CrowdinConfig;
import com.crowdin.client.config.FileBean;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.util.FileUtil;
import com.crowdin.util.PlaceholderUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextLogicTest {
    private CrowdinConfig properties;
    private VirtualFile root;
    private CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache;
    private VirtualFile file;
    private Map<String, FileInfo> filePaths;
    private MockedStatic<FileUtil> fileUtilMockedStatic;
    private MockedStatic<PlaceholderUtil> placeholderUtilMockedStatic;


    @BeforeEach
    void setUp() {
        properties = mock(CrowdinConfig.class);
        root = mock(VirtualFile.class);
        crowdinProjectCache = mock(CrowdinProjectCacheProvider.CrowdinProjectCache.class);
        file = mock(VirtualFile.class);
        filePaths = new HashMap<>();
        fileUtilMockedStatic = mockStatic(FileUtil.class);
        placeholderUtilMockedStatic = mockStatic(PlaceholderUtil.class);
    }

    @AfterEach
    void tearDown() {
        fileUtilMockedStatic.close();
        placeholderUtilMockedStatic.close();
    }

    @Test
    void testFindSourceIdFromSourceFile_PreserveHierarchy_FileFound() {
        when(properties.isPreserveHierarchy()).thenReturn(true);
        when(file.getPath()).thenReturn("/path/to/source/file");
        when(root.getPath()).thenReturn("/path/to");
        when(FileUtil.findRelativePath(root, file)).thenReturn("source/file");
        when(FileUtil.sepAtStart("source/file")).thenReturn("/source/file");

        FileInfo fileInfo = mock(FileInfo.class);
        when(fileInfo.getId()).thenReturn(123L);
        filePaths.put("/source/file", fileInfo);

        Long result = ContextLogic.findSourceIdFromSourceFile(properties, filePaths, file, root);
        assertEquals(123L, result);
    }

    @Test
    void testFindSourceIdFromSourceFile_PreserveHierarchy_FileNotFound() {
        when(properties.isPreserveHierarchy()).thenReturn(true);
        when(file.getPath()).thenReturn("/path/to/source/file");
        when(root.getPath()).thenReturn("/path/to");
        when(FileUtil.findRelativePath(root, file)).thenReturn("source/file");
        when(FileUtil.sepAtStart("source/file")).thenReturn("/source/file");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ContextLogic.findSourceIdFromSourceFile(properties, filePaths, file, root));
        assertTrue(exception.getMessage().contains("Couldn't find any crowdin representative for source file '/source/file'. Skipping"));
    }


    @Test
    void testFindSourceIdFromSourceFile_NoPreserveHierarchy_FileFound() {
        when(properties.isPreserveHierarchy()).thenReturn(false);
        when(file.getPath()).thenReturn("/path/to/source/file");
        when(root.getPath()).thenReturn("/path/to");
        when(FileUtil.findRelativePath(root, file)).thenReturn("source/file");
        when(FileUtil.sepAtStart("source/file")).thenReturn("/source/file");

        FileInfo fileInfo = mock(FileInfo.class);
        when(fileInfo.getId()).thenReturn(123L);
        filePaths.put("file", fileInfo);

        Long result = ContextLogic.findSourceIdFromSourceFile(properties, filePaths, file, root);
        assertEquals(123L, result);
    }

    @Test
    void testFindSourceFileFromTranslationFile_Found() {
        when(file.getPath()).thenReturn("/path/to/translation/file");
        FileBean fileBean = mock(FileBean.class);
        when(properties.getFiles()).thenReturn(List.of(fileBean));
        VirtualFile sourceFile = mock(VirtualFile.class);
        when(FileUtil.getSourceFilesRec(root, fileBean.getSource())).thenReturn(List.of(sourceFile));
        when(FileUtil.getBaseDir(sourceFile, fileBean.getSource())).thenReturn(root);
        when(sourceFile.getName()).thenReturn("sourceFile");
        when(PlaceholderUtil.replaceFilePlaceholders(fileBean.getTranslation(), "sourceFile")).thenReturn("translation/file");
        Language language = mock(Language.class);
        when(crowdinProjectCache.getProjectLanguages()).thenReturn(List.of(language));
        when(PlaceholderUtil.replaceLanguagePlaceholders("translation/file", language, crowdinProjectCache.getLanguageMapping())).thenReturn("translation/file");
        when(root.getPath()).thenReturn("/path/to");

        Optional<Map.Entry<VirtualFile, Language>> result = ContextLogic.findSourceFileFromTranslationFile(file, properties, root, crowdinProjectCache);

        assertTrue(result.isPresent());
        assertEquals(sourceFile, result.get().getKey());
        assertEquals(language, result.get().getValue());
    }

    @Test
    void testFindSourceIdFromSourceFile_NoPreserveHierarchy_FileNotFound() {
        when(properties.isPreserveHierarchy()).thenReturn(false);
        when(file.getPath()).thenReturn("/path/to/source/file");
        when(root.getPath()).thenReturn("/path/to");
        when(FileUtil.findRelativePath(root, file)).thenReturn("source/file");
        when(FileUtil.sepAtStart("source/file")).thenReturn("/source/file");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ContextLogic.findSourceIdFromSourceFile(properties, filePaths, file, root));
        assertTrue(exception.getMessage().contains("Couldn't find any crowdin representative for source file '/source/file'. Skipping"));
    }

    @Test
    void testFindSourceIdFromSourceFile_NoPreserveHierarchy_MultipleFilesFound() {
        when(properties.isPreserveHierarchy()).thenReturn(false);
        when(file.getPath()).thenReturn("/path/to/source/file");
        when(root.getPath()).thenReturn("/path/to");
        when(FileUtil.findRelativePath(root, file)).thenReturn("source/file");
        when(FileUtil.sepAtStart("source/file")).thenReturn("/source/file");

        FileInfo fileInfo1 = mock(FileInfo.class);
        FileInfo fileInfo2 = mock(FileInfo.class);
        filePaths.put("file1", fileInfo1);
        filePaths.put("file2", fileInfo2);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ContextLogic.findSourceIdFromSourceFile(properties, filePaths, file, root));
        assertTrue(exception.getMessage().contains("Couldn't find any crowdin representative for source file '/source/file'. Skipping"));
    }

    @Test
    void testFindSourceFileFromTranslationFile_NotFound() {
        when(file.getPath()).thenReturn("/path/to/translation/file");
        when(properties.getFiles()).thenReturn(List.of());

        Optional<Map.Entry<VirtualFile, Language>> result = ContextLogic.findSourceFileFromTranslationFile(file, properties, root, crowdinProjectCache);

        assertFalse(result.isPresent());
    }
}
