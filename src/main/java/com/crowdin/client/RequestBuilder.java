package com.crowdin.client;

import com.crowdin.client.core.model.PatchOperation;
import com.crowdin.client.core.model.PatchRequest;
import com.crowdin.client.labels.model.AddLabelRequest;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.ExportOptions;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import com.crowdin.client.translations.model.BuildProjectFileTranslationRequest;
import com.crowdin.client.translations.model.CrowdinTranslationCreateProjectBuildForm;
import com.crowdin.client.translations.model.UploadTranslationsRequest;

import java.util.ArrayList;
import java.util.List;

public class RequestBuilder {

    public static UpdateFileRequest updateFile(Long storageId, ExportOptions exportOptions) {
        UpdateFileRequest request = new UpdateFileRequest();
        request.setExportOptions(exportOptions);
        request.setStorageId(storageId);
        return request;
    }

    public static AddFileRequest addFile(
        Long storageId, String name, Long branchId, Long directoryId,
        String type, ExportOptions exportOptions
    ) {
        AddFileRequest request = new AddFileRequest();
        request.setStorageId(storageId);
        request.setName(name);
        request.setBranchId(branchId);
        request.setDirectoryId(directoryId);
        request.setExportOptions(exportOptions);
        return request;
    }

    public static AddBranchRequest addBranch(String name) {
        AddBranchRequest request = new AddBranchRequest();
        request.setName(name);
        return request;
    }

    public static UploadTranslationsRequest uploadTranslation(Long fileId, Long storageId, Boolean importEqSuggestions, Boolean autoApproveImported, Boolean translateHidden) {
        UploadTranslationsRequest request = new UploadTranslationsRequest();
        request.setFileId(fileId);
        request.setStorageId(storageId);
        request.setImportEqSuggestions(importEqSuggestions);
        request.setAutoApproveImported(autoApproveImported);
        request.setTranslateHidden(translateHidden);
        return request;
    }

    public static AddLabelRequest addLabel(String title) {
        AddLabelRequest request = new AddLabelRequest();
        request.setTitle(title);
        return request;
    }

    public static List<PatchRequest> updateExcludedTargetLanguages(List<String> excludedTargetLanguages) {
        List<PatchRequest> request = new ArrayList<>();
        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setPath("/excludedTargetLanguages");
        patchRequest.setOp(PatchOperation.REPLACE);
        patchRequest.setValue(excludedTargetLanguages);
        request.add(patchRequest);
        return request;
    }

    public static CrowdinTranslationCreateProjectBuildForm buildProjectTranslationsRequest(Long branchId) {
        CrowdinTranslationCreateProjectBuildForm request = new CrowdinTranslationCreateProjectBuildForm();
        request.setBranchId(branchId);
        return request;
    }

    public static BuildProjectFileTranslationRequest buildProjectFileTranslation(String targetLanguageId) {
        BuildProjectFileTranslationRequest request = new BuildProjectFileTranslationRequest();
        request.setTargetLanguageId(targetLanguageId);
        return request;
    }
}
