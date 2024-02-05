package com.crowdin.client.config;

import java.util.List;

public class CrowdinConfig {

    private Long projectId;
    private String apiToken;
    private String baseUrl;
    private boolean useGitBranch;
    private boolean preserveHierarchy;
    private List<FileBean> files;
    private boolean debug;
    private boolean autocompletionEnabled;
    private List<String> autocompletionFileExtensions;
    private boolean importEqSuggestions;
    private boolean autoApproveImported;
    private boolean translateHidden;
    private String branch;

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

    public boolean isUseGitBranch() {
        return useGitBranch;
    }

    public void setUseGitBranch(boolean useGitBranch) {
        this.useGitBranch = useGitBranch;
    }

    public boolean isPreserveHierarchy() {
        return preserveHierarchy;
    }

    public void setPreserveHierarchy(Boolean preserveHierarchy) {
        this.preserveHierarchy = preserveHierarchy != null ? preserveHierarchy : false;
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

    public void setDebug(Boolean debug) {
        this.debug = debug != null ? debug : false;
    }

    public boolean isAutocompletionEnabled() {
        return autocompletionEnabled;
    }

    public void setAutocompletionEnabled(boolean autocompletionEnabled) {
        this.autocompletionEnabled = autocompletionEnabled;
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

    public void setImportEqSuggestions(Boolean importEqSuggestions) {
        this.importEqSuggestions = importEqSuggestions != null ? importEqSuggestions : false;
    }

    public boolean isAutoApproveImported() {
        return autoApproveImported;
    }

    public void setAutoApproveImported(Boolean autoApproveImported) {
        this.autoApproveImported = autoApproveImported != null ? autoApproveImported : false;
    }

    public boolean isTranslateHidden() {
        return translateHidden;
    }

    public void setTranslateHidden(Boolean translateHidden) {
        this.translateHidden = translateHidden != null ? translateHidden : false;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
