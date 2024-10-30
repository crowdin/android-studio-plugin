package com.crowdin.client;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.client.bundles.model.BundleExport;
import com.crowdin.client.core.model.PatchRequest;
import com.crowdin.client.labels.model.AddLabelRequest;
import com.crowdin.client.labels.model.Label;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.projectsgroups.model.Project;
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
import com.crowdin.client.translations.model.UploadTranslationsStringsRequest;
import com.crowdin.client.translationstatus.model.FileBranchProgress;
import com.crowdin.client.translationstatus.model.LanguageProgress;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base type for a Crowdin client.
 */
public interface CrowdinClient {

    Long getProjectId();

    String getBaseUrl();

    Long addStorage(String fileName, InputStream content);

    void updateSource(Long sourceId, UpdateFileRequest request);

    URL downloadFile(Long fileId);

    void addSource(AddFileRequest request);

    void editSource(Long fileId, List<PatchRequest> request);

    void uploadTranslation(String languageId, UploadTranslationsRequest request);

    void uploadStringsTranslation(String languageId, UploadTranslationsStringsRequest request);

    Directory addDirectory(AddDirectoryRequest request);

    Project getProject();

    List<Language> extractProjectLanguages(Project crowdinProject);

    UploadStringsProgress uploadStrings(UploadStringsRequest request);

    UploadStringsProgress checkUploadStringsStatus(String id);

    ProjectBuild startBuildingTranslation(BuildProjectTranslationRequest request);

    ProjectBuild checkBuildingStatus(Long buildId);

    URL downloadProjectTranslations(Long buildId);

    BundleExport startBuildingBundle(Long bundleId);

    BundleExport checkBundleBuildingStatus(Long buildId, String exportId);

    URL downloadBundle(Long buildId, String exportId);

    URL downloadFileTranslation(Long fileId, BuildProjectFileTranslationRequest request);

    List<Language> getSupportedLanguages();

    Map<Long, Directory> getDirectories(Long branchId);

    List<FileInfo> getFiles(Long branchId);

    List<SourceString> getStrings();

    Branch addBranch(AddBranchRequest request);

    Optional<Branch> getBranch(String name);

    Map<String, Branch> getBranches();

    List<LanguageProgress> getProjectProgress();

    List<FileBranchProgress> getLanguageProgress(String languageId);

    List<Label> listLabels();

    Label addLabel(AddLabelRequest request);

    List<Bundle> getBundles();
}
