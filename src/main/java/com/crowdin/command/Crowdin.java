package com.crowdin.command;

import com.crowdin.client.Client;
import com.crowdin.client.core.http.exceptions.HttpBadRequestException;
import com.crowdin.client.core.http.exceptions.HttpException;
import com.crowdin.client.core.http.impl.http.ApacheHttpClient;
import com.crowdin.client.core.http.impl.json.JacksonJsonTransformer;
import com.crowdin.client.core.model.ClientConfig;
import com.crowdin.client.core.model.Credentials;
import com.crowdin.client.core.model.ResponseList;
import com.crowdin.client.core.model.ResponseObject;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddDirectoryRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.GeneralFileExportOptions;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import com.crowdin.client.storage.model.Storage;
import com.crowdin.utils.Utils;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ihor on 1/24/17.
 */
public class Crowdin {

    private static final String PROJECT_ID = "project_id";

    private static final String API_TOKEN = "api_token";

    private static final String BASE_URL = "base_url";

    public static final String CROWDIN_DISABLE_BRANCHES = "disable-branches";

    private final Long projectIdentifier;

    private final String errorMessage;

    private final com.crowdin.client.Client client;

    public Crowdin() {
        String errorMessage = null;
        String projectIdentifier = Utils.getPropertyValue(PROJECT_ID, false);
        Long projectId;
        if (!"".equals(projectIdentifier)) {
            try {
                projectId = Long.valueOf(projectIdentifier);
            } catch (NumberFormatException e) {
                projectId = null;
                errorMessage = "Invalid project id";
            }
        } else {
            errorMessage = "Project id is empty";
            projectId = null;
        }
        this.projectIdentifier = projectId;

        String apiToken = Utils.getPropertyValue(API_TOKEN, false);
        String apiToken1;
        if (!"".equals(apiToken)) {
            apiToken1 = apiToken;
        } else {
            errorMessage = "Api token is empty";
            apiToken1 = null;
        }

        String baseUrl = Utils.getPropertyValue(BASE_URL, false);
        String organization;
        if (!"".equals(baseUrl)) {
            if ((baseUrl.endsWith(".crowdin.com") || baseUrl.endsWith(".crowdin.com/")) && baseUrl.startsWith("https://")) {
                //enterprise
                organization = baseUrl.substring(8).split(".crowdin.com")[0];
            } else if (baseUrl.startsWith("https://crowdin.com")) {
                //standard
                organization = null;
            } else {
                organization = null;
                //unknown url
                errorMessage = "Invalid base url";
            }
        } else {
            organization = null;
        }

        this.errorMessage = errorMessage;
        if (this.errorMessage == null) {
            Credentials credentials = new Credentials(apiToken1, organization);
            ClientConfig clientConfig = ClientConfig.builder()
                    .userAgent("android-studio-plugin")
                    .httpClient(new ApacheHttpClient(credentials, new JacksonJsonTransformer(), Collections.emptyMap()))
                    .build();
            this.client = new Client(credentials, clientConfig);
        } else {
            Utils.showErrorMessage(this.errorMessage);
            this.client = null;
        }
    }

    public void uploadFile(VirtualFile source, String branch) {
        if (source == null || this.errorMessage != null) {
            return;
        }

        try {
            Long branchId = this.getOrCreateBranch(branch);

            List<String> folders = Stream.of(source.getCanonicalPath().split(File.separator))
                    .filter(p -> p.length() > 0)
                    .collect(Collectors.toList());
            Long parentId = null;
            for (int i = 0; i < folders.size() - 1; i++) {
                String folder = folders.get(i);
                try {
                    Long directory = this.findDirectory(folder, parentId, branchId);
                    if (directory != null) {
                        parentId = directory;
                    } else {
                        AddDirectoryRequest request = new AddDirectoryRequest();
                        request.setBranchId(branchId);
                        request.setDirectoryId(parentId);
                        request.setName(folder);
                        ResponseObject<Directory> directoryResponseObject = this.client.getSourceFilesApi().addDirectory(this.projectIdentifier, request);
                        parentId = directoryResponseObject.getData().getId();
                    }
                } catch (Exception error) {
                    if (!this.concurrentIssue(error)) {
                        throw error;
                    }
                    parentId = this.waitAndFindDirectory(folder, parentId, branchId);
                }
            }

            ResponseObject<Storage> storageResponseObject = this.client.getStorageApi().addStorage(source.getName(), source.getInputStream());
            Long storageId = storageResponseObject.getData().getId();
            ResponseList<com.crowdin.client.sourcefiles.model.File> fileResponseList = this.client.getSourceFilesApi().listFiles(this.projectIdentifier, null, parentId, null, 500, null);
            ResponseObject<com.crowdin.client.sourcefiles.model.File> foundFile = fileResponseList.getData().stream()
                    .filter(f -> {
                        if (branchId != null) {
                            return f.getData().getBranchId().equals(branchId);
                        } else {
                            return f.getData().getBranchId() == null;
                        }
                    })
                    .filter(f -> f.getData().getName().equals(source.getName()))
                    .findFirst().orElse(null);
            if (foundFile != null) {
                UpdateFileRequest request = new UpdateFileRequest();
                request.setStorageId(storageId);
                GeneralFileExportOptions generalFileExportOptions = new GeneralFileExportOptions();
                generalFileExportOptions.setExportPattern("/values-%android_code%/%original_file_name%");
                this.client.getSourceFilesApi().updateOrRestoreFile(this.projectIdentifier, foundFile.getData().getId(), request);
            } else {
                AddFileRequest request = new AddFileRequest();
                request.setStorageId(storageId);
                request.setDirectoryId(parentId);
                request.setName(source.getName());
                GeneralFileExportOptions generalFileExportOptions = new GeneralFileExportOptions();
                generalFileExportOptions.setExportPattern("/values-%android_code%/%original_file_name%");
                request.setExportOptions(generalFileExportOptions);
                this.client.getSourceFilesApi().addFile(this.projectIdentifier, request);
            }
        } catch (Exception e) {
            Utils.showErrorMessage(this.errorMessage);
        }
    }

    public File downloadTranslations(VirtualFile sourceFile, String branch) {
        if (this.errorMessage != null) {
            return null;
        }
        //TODO re-implement
//        ClientResponse clientResponse;
//        Credentials credentials = new Credentials(baseUrl, projectIdentifier, projectKey, null);
//        CrowdinApiParametersBuilder crowdinApiParametersBuilder = new CrowdinApiParametersBuilder();
//        CrowdinApiClient crowdinApiClient = new Crwdn();
//        crowdinApiParametersBuilder.json()
//                .headers(HttpHeaders.USER_AGENT, USER_AGENT_ANDROID_STUDIO_PLUGIN)
//                .downloadPackage("all")
//                .destinationFolder(sourceFile.getParent().getParent().getCanonicalPath() + "/");
//        if (branch != null && !branch.isEmpty()) {
//            crowdinApiParametersBuilder.branch(branch);
//        }
//        try {
//            clientResponse = crowdinApiClient.downloadTranslations(credentials, crowdinApiParametersBuilder);
//            //LOGGER.info("Crowdin: export translations " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
//            System.out.println("Crowdin: download translations " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
//            System.out.println(clientResponse.getEntity(String.class));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return new File(sourceFile.getParent().getParent().getCanonicalPath() + "/all.zip");
    }

    private Long getOrCreateBranch(String name) {
        if (name != null && name.length() > 0) {
            try {
                List<ResponseObject<Branch>> branches = this.client.getSourceFilesApi().listBranches(this.projectIdentifier, name, 500, null).getData();
                Branch foundBranch = branches.stream()
                        .filter(e -> e.getData().getName().equalsIgnoreCase(name))
                        .map(ResponseObject::getData)
                        .findFirst().orElse(null);
                if (foundBranch != null) {
                    return foundBranch.getId();
                } else {
                    AddBranchRequest request = new AddBranchRequest();
                    request.setName(name);
                    ResponseObject<Branch> responseObject = this.client.getSourceFilesApi().addBranch(this.projectIdentifier, request);
                    return responseObject.getData().getId();
                }
            } catch (Exception e) {
                try {
                    if (!this.concurrentIssue(e)) {
                        throw e;
                    }
                    return this.waitAndFindBranch(name);
                } catch (Exception error) {
                    String msg = "Failed to create/find branch for project " + this.projectIdentifier + ". " + this.getErrorMessage(error);
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
        return Utils.retry(() -> {
            ResponseList<Branch> branchResponseList = this.client.getSourceFilesApi().listBranches(this.projectIdentifier, name, 500, null);
            ResponseObject<Branch> branchResponseObject = branchResponseList.getData().stream()
                    .filter(branch -> branch.getData().getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (branchResponseObject != null) {
                return branchResponseObject.getData().getId();
            } else {
                throw new Exception("Could not find branch " + name + "in Crowdin response");
            }
        }, 3);
    }

    private Long waitAndFindDirectory(String name, Long parent, Long branchId) throws Exception {
        return Utils.retry(() -> {
            Long directory = this.findDirectory(name, parent, branchId);
            if (directory != null) {
                return directory;
            } else {
                throw new Exception("Could not find directory " + name + "in Crowdin response");
            }
        }, 3);
    }

    private Long findDirectory(String name, Long parent, Long branchId) {
        ResponseList<Directory> directoryResponseList = this.client.getSourceFilesApi().listDirectories(this.projectIdentifier, null, parent, null, 500, null);
        ResponseObject<Directory> foundDir = directoryResponseList.getData()
                .stream()
                .filter(dir -> {
                    if (branchId == null) {
                        return dir.getData().getBranchId() == null;
                    } else {
                        return dir.getData().getBranchId().equals(branchId);
                    }
                })
                .filter(dir -> dir.getData().getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (foundDir != null) {
            return foundDir.getData().getId();
        } else {
            return null;
        }
    }
}
