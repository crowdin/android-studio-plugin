package com.crowdin.client;

import java.util.List;

public class CrowdinProperties {

    private Long projectId;
    private String apiToken;
    private String baseUrl;
    private boolean disabledBranches;
    private boolean preserveHierarchy;
    private List<FileBean> files;
    private boolean debug;
    private boolean autocompletionDisabled;
    private List<String> autocompletionFileExtensions;
    private boolean importEqSuggestions;
    private boolean autoApproveImported;
    private boolean translateHidden;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isDisabledBranches() {
        return disabledBranches;
    }

    public void setDisabledBranches(boolean disabledBranches) {
        this.disabledBranches = disabledBranches;
    }

    public boolean isPreserveHierarchy() {
        return preserveHierarchy;
    }

    public void setPreserveHierarchy(boolean preserveHierarchy) {
        this.preserveHierarchy = preserveHierarchy;
    }

    public List<FileBean> getFiles() {
        return files;
    }

    public void setFiles(List<FileBean> files) {
        this.files = files;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isAutocompletionDisabled() {
        return autocompletionDisabled;
    }

    public void setAutocompletionDisabled(boolean autocompletionDisabled) {
        this.autocompletionDisabled = autocompletionDisabled;
    }

    public List<String> getAutocompletionFileExtensions() {
        return autocompletionFileExtensions;
    }

    public void setAutocompletionFileExtensions(List<String> autocompletionFileExtensions) {
        this.autocompletionFileExtensions = autocompletionFileExtensions;
    }

    public boolean isImportEqSuggestions() {
        return importEqSuggestions;
    }

    public void setImportEqSuggestions(boolean importEqSuggestions) {
        this.importEqSuggestions = importEqSuggestions;
    }

    public boolean isAutoApproveImported() {
        return autoApproveImported;
    }

    public void setAutoApproveImported(boolean autoApproveImported) {
        this.autoApproveImported = autoApproveImported;
    }

    public boolean isTranslateHidden() {
        return translateHidden;
    }

    public void setTranslateHidden(boolean translateHidden) {
        this.translateHidden = translateHidden;
    }
}
