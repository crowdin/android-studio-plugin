package com.crowdin.client;

import com.crowdin.client.core.http.exceptions.HttpBadRequestException;
import com.crowdin.client.core.http.exceptions.HttpException;
import com.crowdin.client.core.http.impl.http.ApacheHttpClient;
import com.crowdin.client.core.http.impl.json.JacksonJsonTransformer;
import com.crowdin.client.core.model.ClientConfig;
import com.crowdin.client.core.model.Credentials;
import com.crowdin.client.core.model.DownloadLink;
import com.crowdin.client.core.model.ResponseList;
import com.crowdin.client.core.model.ResponseObject;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.GeneralFileExportOptions;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import com.crowdin.client.storage.model.Storage;
import com.crowdin.client.translations.model.BuildProjectTranslationRequest;
import com.crowdin.client.translations.model.ProjectBuild;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.RetryUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Crowdin {

    private final Long projectId;

    private final boolean invalidConfiguration;

    private final com.crowdin.client.Client client;

    public Crowdin(@NotNull Project project) {
        CrowdinClientProperties crowdinClientProperties = CrowdinClientProperties.load(project);
        if (crowdinClientProperties.getErrorMessage() != null) {
            this.projectId = null;
            this.client = null;
            this.invalidConfiguration = true;
            NotificationUtil.showErrorMessage(crowdinClientProperties.getErrorMessage());
        } else {
            this.invalidConfiguration = false;
            this.projectId = crowdinClientProperties.getProjectId();
            Credentials credentials = new Credentials(crowdinClientProperties.getToken(), null, crowdinClientProperties.getBaseUrl());
            ClientConfig clientConfig = ClientConfig.builder()
                    .userAgent("crowdin-android-studio-plugin/ " + PluginManager.getPlugin(PluginId.getId(PluginManager.CORE_PLUGIN_ID)).getVersion() + " android-studio/" + PluginManager.getPlugin(PluginId.getId("com.crowdin.crowdin-idea")).getVersion())
                    .httpClient(new ApacheHttpClient(credentials, new JacksonJsonTransformer(), Collections.emptyMap()))
                    .build();
            this.client = new Client(credentials, clientConfig);
        }
    }

    public void uploadFile(VirtualFile source, String branch) {
        if (source == null || this.invalidConfiguration) {
            return;
        }

        try {
            Long branchId = this.getOrCreateBranch(branch);

            ResponseObject<Storage> storageResponseObject = this.client.getStorageApi().addStorage(source.getName(), source.getInputStream());
            Long storageId = storageResponseObject.getData().getId();
            ResponseList<com.crowdin.client.sourcefiles.model.File> fileResponseList = this.client.getSourceFilesApi().listFiles(this.projectId, branchId, null, null, 500, null);
            ResponseObject<com.crowdin.client.sourcefiles.model.File> foundFile = fileResponseList.getData().stream()
                    .filter(f -> f.getData().getName().equals(source.getName()))
                    .findFirst().orElse(null);
            if (foundFile != null) {
                UpdateFileRequest request = new UpdateFileRequest();
                request.setStorageId(storageId);
                GeneralFileExportOptions generalFileExportOptions = new GeneralFileExportOptions();
                generalFileExportOptions.setExportPattern("/values-%android_code%/%original_file_name%");
                this.client.getSourceFilesApi().updateOrRestoreFile(this.projectId, foundFile.getData().getId(), request);
            } else {
                AddFileRequest request = new AddFileRequest();
                request.setStorageId(storageId);
                request.setName(source.getName());
                GeneralFileExportOptions generalFileExportOptions = new GeneralFileExportOptions();
                generalFileExportOptions.setExportPattern("/values-%android_code%/%original_file_name%");
                request.setExportOptions(generalFileExportOptions);
                this.client.getSourceFilesApi().addFile(this.projectId, request);
            }
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(this.getErrorMessage(e));
        }
    }

    public File downloadTranslations(VirtualFile sourceFile, String branch) {
        if (this.invalidConfiguration) {
            return null;
        }
        try {
            Long branchId = this.getOrCreateBranch(branch);

            BuildProjectTranslationRequest buildProjectTranslationRequest = new BuildProjectTranslationRequest();
            buildProjectTranslationRequest.setBranchId(branchId);
            ResponseObject<ProjectBuild> projectBuildResponseObject = this.client.getTranslationsApi().buildProjectTranslation(this.projectId, buildProjectTranslationRequest);
            Long buildId = projectBuildResponseObject.getData().getId();

            boolean finished = false;
            while (!finished) {
                ResponseObject<ProjectBuild> projectBuildStatusResponseObject = this.client.getTranslationsApi().checkBuildStatus(this.projectId, buildId);
                finished = projectBuildStatusResponseObject.getData().getStatus().equalsIgnoreCase("finished");
            }

            ResponseObject<DownloadLink> downloadLinkResponseObject = this.client.getTranslationsApi().downloadProjectTranslations(this.projectId, buildId);
            String link = downloadLinkResponseObject.getData().getUrl();

            File file = new File(sourceFile.getParent().getParent().getCanonicalPath() + "/all.zip");
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(link).openStream()); FileOutputStream fos = new FileOutputStream(file)) {
                fos.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            return file;
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(this.getErrorMessage(e));
            return null;
        }
    }


    private Long getOrCreateBranch(String name) {
        if (name != null && name.length() > 0) {
            try {
                List<ResponseObject<Branch>> branches = this.client.getSourceFilesApi().listBranches(this.projectId, name, 500, null).getData();
                Branch foundBranch = branches.stream()
                        .filter(e -> e.getData().getName().equalsIgnoreCase(name))
                        .map(ResponseObject::getData)
                        .findFirst().orElse(null);
                if (foundBranch != null) {
                    return foundBranch.getId();
                } else {
                    AddBranchRequest request = new AddBranchRequest();
                    request.setName(name);
                    ResponseObject<Branch> responseObject = this.client.getSourceFilesApi().addBranch(this.projectId, request);
                    return responseObject.getData().getId();
                }
            } catch (Exception e) {
                try {
                    if (!this.concurrentIssue(e)) {
                        throw e;
                    }
                    return this.waitAndFindBranch(name);
                } catch (Exception error) {
                    String msg = "Failed to create/find branch for project " + this.projectId + ". " + this.getErrorMessage(error);
                    throw new Error(msg);
                }
            }
        }
        return null;
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof HttpException) {
            return ((HttpException) e).getError().getMessage();
        } else if (e instanceof HttpBadRequestException) {
            return ((HttpBadRequestException) e).getErrors().stream()
                    .map(er -> {
                        String key = er.getError().getKey() == null ? "" : er.getError().getKey();
                        return key + " " + er.getError().getErrors().stream()
                                .map(err -> err.getCode() + " " + err.getMessage())
                                .collect(Collectors.joining(";"));
                    })
                    .collect(Collectors.joining(";"));
        } else {
            return e.getMessage();
        }
    }

    private boolean concurrentIssue(Exception error) {
        return this.codeExists(error, "notUnique") || this.codeExists(error, "parallelCreation");
    }

    private boolean codeExists(Exception e, String code) {
        if (e instanceof HttpException) {
            return ((HttpException) e).getError().getCode().equalsIgnoreCase(code);
        } else if (e instanceof HttpBadRequestException) {
            return ((HttpBadRequestException) e).getErrors().stream()
                    .anyMatch(error -> error.getError().getErrors().stream()
                            .anyMatch(er -> er.getCode().equalsIgnoreCase(code))
                    );
        } else {
            return false;
        }
    }

    private Long waitAndFindBranch(String name) throws Exception {
        return RetryUtil.retry(() -> {
            ResponseList<Branch> branchResponseList = this.client.getSourceFilesApi().listBranches(this.projectId, name, 500, null);
            ResponseObject<Branch> branchResponseObject = branchResponseList.getData().stream()
                    .filter(branch -> branch.getData().getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (branchResponseObject != null) {
                return branchResponseObject.getData().getId();
            } else {
                throw new Exception("Could not find branch " + name + "in Crowdin response");
            }
        });
    }

}
