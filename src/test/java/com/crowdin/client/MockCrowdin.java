package com.crowdin.client;

import static java.util.Collections.singletonMap;

import com.crowdin.api.model.BranchBuilder;
import com.crowdin.api.model.LanguageBuilder;
import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.client.bundles.model.BundleExport;
import com.crowdin.client.core.model.PatchRequest;
import com.crowdin.client.labels.model.AddLabelRequest;
import com.crowdin.client.labels.model.Label;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.projectsgroups.model.Project;
import com.crowdin.client.projectsgroups.model.ProjectSettings;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddDirectoryRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import com.crowdin.client.sourcestrings.model.SourceString;
import com.crowdin.client.sourcestrings.model.UploadStringsProgress;
import com.crowdin.client.sourcestrings.model.UploadStringsRequest;
import com.crowdin.client.translations.model.BuildProjectFileTranslationRequest;
import com.crowdin.client.translations.model.BuildProjectTranslationRequest;
import com.crowdin.client.translations.model.ProjectBuild;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.client.translationstatus.model.FileBranchProgress;
import com.crowdin.client.translationstatus.model.LanguageProgress;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mock Crowdin client for testing purposes, so that real HTTP requests are not sent to Crowdin.
 */
public class MockCrowdin implements CrowdinClient {

    static final Branch STANDARD_BRANCH_1 = BranchBuilder.standard().setIdentifiers("branch1", 1L).build();
    static final Branch STANDARD_BRANCH_2 = BranchBuilder.standard().setIdentifiers("branch2", 2L).build();

    private final Project project;

    public MockCrowdin(@NotNull Long projectId) {
        if (projectId == 2L) {
            ProjectSettings projectSettings = new ProjectSettings();
            projectSettings.setLanguageMapping(singletonMap("hu", singletonMap("placeholder", "value")));
            project = projectSettings;
        } else {
            project = new Project();
        }
    }

    @Override
    public Long addStorage(String fileName, InputStream content) {
        return null;
    }

    @Override
    public void updateSource(Long sourceId, UpdateFileRequest request) {
    }

    @Override
    public URL downloadFile(Long fileId) {
        return null;
    }

    @Override
    public void addSource(AddFileRequest request) {
    }

    @Override
    public void editSource(Long fileId, List<PatchRequest> request) {
    }

    @Override
    public void uploadTranslation(String languageId, UploadTranslationsRequest request) {
    }

    @Override
    public Directory addDirectory(AddDirectoryRequest request) {
        return null;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public List<Language> extractProjectLanguages(Project crowdinProject) {
        return Collections.singletonList(LanguageBuilder.DEU.build());
    }

    @Override
    public UploadStringsProgress uploadStrings(UploadStringsRequest request) {
        return null;
    }

    @Override
    public UploadStringsProgress checkUploadStringsStatus(String id) {
        return null;
    }

    @Override
    public ProjectBuild startBuildingTranslation(BuildProjectTranslationRequest request) {
        return null;
    }

    @Override
    public ProjectBuild checkBuildingStatus(Long buildId) {
        return null;
    }

    @Override
    public URL downloadProjectTranslations(Long buildId) {
        return null;
    }

    @Override
    public BundleExport startBuildingBundle(Long bundleId) {
        return null;
    }

    @Override
    public BundleExport checkBundleBuildingStatus(Long buildId, String exportId) {
        return null;
    }

    @Override
    public URL downloadBundle(Long buildId, String exportId) {
        return null;
    }

    @Override
    public URL downloadFileTranslation(Long fileId, BuildProjectFileTranslationRequest request) {
        return null;
    }

    @Override
    public List<Language> getSupportedLanguages() {
        return Collections.singletonList(LanguageBuilder.ENG.build());
    }

    @Override
    public Map<Long, Directory> getDirectories(Long branchId) {
        Directory dir = new Directory();
        dir.setName("dirname");
        return singletonMap(10L, dir);
    }

    @Override
    public List<FileInfo> getFiles(Long branchId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName("filename");
        return Collections.singletonList(fileInfo);
    }

    @Override
    public List<SourceString> getStrings() {
        return Collections.singletonList(new SourceString());
    }

    @Override
    public Branch addBranch(AddBranchRequest request) {
        return null;
    }

    @Override
    public Optional<Branch> getBranch(String name) {
        return Optional.empty();
    }

    @Override
    public Map<String, Branch> getBranches() {
        Map<String, Branch> branches = new HashMap<>();
        branches.put("branch1", STANDARD_BRANCH_1);
        branches.put("branch2", STANDARD_BRANCH_2);
        return branches;
    }

    @Override
    public List<LanguageProgress> getProjectProgress() {
        return null;
    }

    @Override
    public List<FileBranchProgress> getLanguageProgress(String languageId) {
        return null;
    }

    @Override
    public List<Label> listLabels() {
        return null;
    }

    @Override
    public Label addLabel(AddLabelRequest request) {
        return null;
    }

    @Override
    public List<Bundle> getBundles() {
        return null;
    }
}
