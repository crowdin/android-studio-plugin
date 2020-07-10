package com.crowdin.client;

import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.ExportOptions;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import com.crowdin.client.translations.model.UploadTranslationsRequest;

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

    public static UploadTranslationsRequest uploadTranslation(Long fileId, Long storageId) {
        UploadTranslationsRequest request = new UploadTranslationsRequest();
        request.setFileId(fileId);
        request.setStorageId(storageId);
        return request;
    }
}
