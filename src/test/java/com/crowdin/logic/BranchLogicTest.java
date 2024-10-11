package com.crowdin.logic;

import com.crowdin.client.BranchInfo;
import com.crowdin.client.Crowdin;
import com.crowdin.client.config.CrowdinConfig;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.util.CrowdinFileUtil;
import com.crowdin.util.GitUtil;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchLogicTest {

    @Mock
    private Crowdin crowdin;

    @Mock
    private Project project;

    @Mock
    private CrowdinConfig properties;

    @Mock
    private CrowdinProjectCacheProvider.CrowdinProjectCache projectCache;

    @InjectMocks
    private BranchLogic branchLogic;

    private MockedStatic<GitUtil> gitUtilMock;
    private MockedStatic<CrowdinFileUtil> crowdinFileUtilMock;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        branchLogic = new BranchLogic(crowdin, project, properties);
        gitUtilMock = mockStatic(GitUtil.class);
        crowdinFileUtilMock = mockStatic(CrowdinFileUtil.class);
    }

    @AfterEach
    void teardown() {
        gitUtilMock.close();
        crowdinFileUtilMock.close();
    }

    @Test
    void testAcquireBranchName_UseGitBranch() {
        when(properties.isUseGitBranch()).thenReturn(true);
        BranchInfo mockBranchInfo = new BranchInfo("feature-branch", "feature-branch");
        when(GitUtil.getCurrentBranch(project)).thenReturn(mockBranchInfo);
        when(CrowdinFileUtil.isValidBranchName("feature-branch")).thenReturn(true);

        when(GitUtil.getCurrentBranch(project)).thenReturn(mockBranchInfo);
        when(CrowdinFileUtil.isValidBranchName("feature-branch")).thenReturn(true);

        String branchName = branchLogic.acquireBranchName();

        assertEquals("feature-branch", branchName);
    }

    @Test
    void testAcquireBranchName_UseConfiguredBranch() {
        when(properties.isUseGitBranch()).thenReturn(false);
        when(properties.getBranch()).thenReturn("configured-branch");
        when(CrowdinFileUtil.isValidBranchName("configured-branch")).thenReturn(true);

        String branchName = branchLogic.acquireBranchName();

        assertEquals("configured-branch", branchName);
    }

    @Test
    void testAcquireBranchName_InvalidBranchName() {
        when(properties.isUseGitBranch()).thenReturn(false);
        when(properties.getBranch()).thenReturn("invalid/branch");
        when(CrowdinFileUtil.isValidBranchName("invalid/branch")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> branchLogic.acquireBranchName());

        assertEquals("Branch name can't contain any of the following characters: \\ / : * ? \" < > |", exception.getMessage());
    }

    @Test
    void testAcquireBranchName_EmptyBranchName() {
        when(properties.isUseGitBranch()).thenReturn(false);
        when(properties.getBranch()).thenReturn("");
        when(CrowdinFileUtil.isValidBranchName("")).thenReturn(true);

        String branchName = branchLogic.acquireBranchName();

        assertEquals("", branchName);
    }

    @Test
    void testGetBranch_NonExistingBranch_DoNotCreate() {
        when(projectCache.getBranches()).thenReturn(Collections.emptyMap());
        when(properties.isUseGitBranch()).thenReturn(false);
        when(properties.getBranch()).thenReturn("non-existing-branch");
        when(CrowdinFileUtil.isValidBranchName("non-existing-branch")).thenReturn(true);

        branchLogic.acquireBranchName();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> branchLogic.getBranch(projectCache, false));

        assertEquals("Branch 'non-existing-branch' does not exists in Crowdin. Try switching to another branch locally", exception.getMessage());
        verify(projectCache).getBranches();
        verifyNoMoreInteractions(crowdin);
    }
}