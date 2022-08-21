package com.crowdin.client;

import com.crowdin.client.core.http.exceptions.HttpBadRequestException;
import com.crowdin.client.core.http.exceptions.HttpException;
import com.crowdin.client.core.model.*;
import com.crowdin.client.labels.model.AddLabelRequest;
import com.crowdin.client.labels.model.Label;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.*;
import com.crowdin.client.sourcestrings.model.SourceString;
import com.crowdin.client.translations.model.BuildProjectFileTranslationRequest;
import com.crowdin.client.translations.model.BuildProjectTranslationRequest;
import com.crowdin.client.translations.model.ProjectBuild;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.client.translationstatus.model.FileProgress;
import com.crowdin.client.translationstatus.model.LanguageProgress;
import com.crowdin.util.RetryUtil;
import com.crowdin.util.Util;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class Crowdin {

    private final Long projectId;

    private final Project project;

    private final com.crowdin.client.Client client;

    public Crowdin(@NotNull Project project, @NotNull Long projectId, @NotNull String apiToken, String baseUrl) {
        this.project = project;
        this.projectId = projectId;
        Credentials credentials = new Credentials(apiToken, null, baseUrl);
        ClientConfig clientConfig = ClientConfig.builder()
            .userAgent(Util.getUserAgent())
            .build();
        this.client = new Client(credentials, clientConfig);
    }

    public Long addStorage(String fileName, InputStream content) {
        return executeRequest(() -> this.client.getStorageApi()
            .addStorage(fileName, content)
            .getData()
            .getId());
    }

    public void updateSource(Long sourceId, UpdateFileRequest request) {
        executeRequest(() -> this.client.getSourceFilesApi()
            .updateOrRestoreFile(this.projectId, sourceId, request));
    }

    public URL downloadFile(Long fileId) {
        return url(executeRequest(() -> this.client.getSourceFilesApi()
            .downloadFile(this.projectId, fileId)
            .getData()));
    }

    public void addSource(AddFileRequest request) {
        executeRequest(() -> this.client.getSourceFilesApi()
            .addFile(this.projectId, request));
    }

    public void editSource(Long fileId, List<PatchRequest> request) {
        executeRequest(() -> this.client.getSourceFilesApi()
            .editFile(this.projectId, fileId, request));
    }

    public void uploadTranslation(String languageId, UploadTranslationsRequest request) {
        executeRequest(() -> this.client.getTranslationsApi()
            .uploadTranslations(this.projectId, languageId, request));
    }

    public Directory addDirectory(AddDirectoryRequest request) {
        return executeRequest(() -> this.client.getSourceFilesApi()
            .addDirectory(this.projectId, request)
            .getData());
    }

    public com.crowdin.client.projectsgroups.model.Project getProject() {
        return executeRequest(() -> this.client.getProjectsGroupsApi()
            .getProject(this.projectId)
            .getData());
    }

    public List<Language> extractProjectLanguages(com.crowdin.client.projectsgroups.model.Project crowdinProject) {
        return crowdinProject.getTargetLanguages();
    }

    public ProjectBuild startBuildingTranslation(BuildProjectTranslationRequest request) {
        return executeRequest(() -> this.client.getTranslationsApi()
            .buildProjectTranslation(this.projectId, request)
            .getData());
    }

    public ProjectBuild checkBuildingStatus(Long buildId) {
        return executeRequest(() -> this.client.getTranslationsApi()
            .checkBuildStatus(projectId, buildId)
            .getData());
    }

    public URL downloadProjectTranslations(Long buildId) {
        return url(executeRequest(() -> this.client.getTranslationsApi()
            .downloadProjectTranslations(this.projectId, buildId)
            .getData()));
    }

    public URL downloadFileTranslation(Long fileId, BuildProjectFileTranslationRequest request) {
        return url(executeRequest(() -> client.getTranslationsApi()
            .buildProjectFileTranslation(this.projectId, fileId, null, request)
            .getData()));
    }

    public List<Language> getSupportedLanguages() {
        return executeRequest(() -> client.getLanguagesApi().listSupportedLanguages(500, 0)
            .getData()
            .stream()
            .map(ResponseObject::getData)
            .collect(Collectors.toList()));
    }

    public Map<Long, Directory> getDirectories(Long branchId) {
        return executeRequestFullList((limit, offset) ->
                this.client.getSourceFilesApi()
                    .listDirectories(this.projectId, branchId, null, true, limit, offset)
                    .getData()
            )
            .stream()
            .map(ResponseObject::getData)
            .filter(dir -> Objects.equals(dir.getBranchId(), branchId))
            .collect(Collectors.toMap(Directory::getId, Function.identity()));
    }

    public List<com.crowdin.client.sourcefiles.model.FileInfo> getFiles(Long branchId) {
        return executeRequestFullList((limit, offset) ->
                this.client.getSourceFilesApi()
                    .listFiles(this.projectId, branchId, null, null, true, 500, 0)
                    .getData()
            )
            .stream()
            .map(ResponseObject::getData)
            .filter(file -> Objects.equals(file.getBranchId(), branchId))
            .collect(Collectors.toList());
    }

    public List<SourceString> getStrings() {
        return executeRequestFullList((limit, offset) ->
                this.client.getSourceStringsApi().listSourceStrings(
                        this.projectId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        limit,
                        offset).getData()
                )
                .stream()
                .map(ResponseObject::getData)
                .collect(Collectors.toList());
    }

    /**
     * @param request represents function that downloads list of models and has two args (limit, offset)
     * @param <T> represents model
     * @return list of models accumulated from request function
     */
    private <T> List<T> executeRequestFullList(BiFunction<Integer, Integer, List<T>> request) {
        List<T> models = new ArrayList<>();
        long counter;
        int limit = 500;
        do {
            List<T> responseModels = executeRequest(() -> request.apply(limit, models.size()));
            models.addAll(responseModels);
            counter = responseModels.size();
        } while (counter == limit);
        return models;
    }

    public Branch addBranch(AddBranchRequest request) {
        try {
            return executeRequest(() -> this.client.getSourceFilesApi()
                .addBranch(this.projectId, request)
                .getData());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("regexNotMatch File name can't contain")) {
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.branch_contains_forbidden_symbols"));
            } else {
                throw e;
            }
        }
    }

    public Optional<Branch> getBranch(String name) {
        List<ResponseObject<Branch>> branches = executeRequest(() -> this.client.getSourceFilesApi().listBranches(this.projectId, name, 500, null).getData());
        return branches.stream()
                .filter(e -> e.getData().getName().equalsIgnoreCase(name))
                .map(ResponseObject::getData)
                .findFirst();
    }

    public Map<String, Branch> getBranches() {
        return executeRequestFullList((limit, offset) ->
            this.client.getSourceFilesApi()
                .listBranches(this.projectId, null, limit, offset)
                .getData()
        )
            .stream()
            .map(ResponseObject::getData)
            .collect(Collectors.toMap(Branch::getName, Function.identity()));
    }

    public List<LanguageProgress> getProjectProgress() {
        return executeRequestFullList((limit, offset) -> this.client.getTranslationStatusApi()
            .getProjectProgress(this.projectId, limit, offset, null)
            .getData()
            .stream()
            .map(ResponseObject::getData)
            .collect(Collectors.toList()));
    }

    public List<FileProgress> getLanguageProgress(String languageId) {
        return executeRequestFullList((limit, offset) -> this.client.getTranslationStatusApi()
            .getLanguageProgress(this.projectId, languageId, limit, offset)
            .getData()
            .stream()
            .map(ResponseObject::getData)
            .collect(Collectors.toList()));
    }

    public List<Label> listLabels() {
        return executeRequestFullList((limit, offset) -> this.client.getLabelsApi()
            .listLabels(this.projectId, limit, offset)
            .getData()
            .stream()
            .map(ResponseObject::getData)
            .collect(Collectors.toList()));
    }

    public Label addLabel(AddLabelRequest request) {
        return executeRequest(() ->this.client.getLabelsApi()
            .addLabel(this.projectId, request)
            .getData());
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
                throw new Exception(String.format(MESSAGES_BUNDLE.getString("errors.find_branch"), name));
            }
        });
    }

    private boolean customMessage(Exception e) {
        if (e instanceof HttpException) {
            HttpException ex = (HttpException) e;
            if (ex.getError().getCode().equalsIgnoreCase("401")) {
                return true;
            }
        }
        return false;
    }

    private URL url(DownloadLink downloadLink) {
        try {
            return new URL(downloadLink.getUrl());
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception: malformed download url: " + downloadLink.getUrl(), e);
        }
    }

    private <T> T executeRequest(Supplier<T> exec) {
        try {
            return exec.get();
        } catch (HttpException e) {
            HttpException ex = (HttpException) e;
            String code = (ex.getError() != null && ex.getError().getCode() != null) ? ex.getError().getCode() : "<empty_code>";
            String message = (ex.getError() != null && ex.getError().getMessage() != null) ? ex.getError().getMessage() : "<empty_message>";
            String errorMessage = ("401".equals(code))
                ? MESSAGES_BUNDLE.getString("errors.authorize")
                : String.format("Error from server: <Code: %s, Message: %s>", code, message);
            throw new RuntimeException(errorMessage, e);
        } catch (HttpBadRequestException e) {
            String errorMessage;
            if (e.getErrors() == null) {
                errorMessage = "Wrong parameters: <Key: <empty_key>, Code: <empty_code>, Message: <empty_message>";
            } else {
                errorMessage = "Wrong parameters: \n" + e.getErrors()
                    .stream()
                    .map(HttpBadRequestException.ErrorHolder::getError)
                    .flatMap(holder -> holder.getErrors()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(error ->
                            String.format("<Key: %s, Code: %s, Message: %s>",
                                (holder.getKey() != null) ? holder.getKey() : "<empty_key>",
                                (error.getCode() != null) ? error.getCode() : "<empty_code>",
                                (error.getMessage() != null) ? error.getMessage() : "<empty_message>")))
                    .collect(Collectors.joining("\n"));
            }
            throw new RuntimeException(errorMessage, e);
        }
    }

}
