package com.crowdin.service;

import com.crowdin.client.CrowdinClient;
import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.projectsgroups.model.Project;
import com.crowdin.client.projectsgroups.model.ProjectSettings;
import com.crowdin.client.projectsgroups.model.Type;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.sourcestrings.model.SourceString;
import com.crowdin.util.CrowdinFileUtil;
import com.crowdin.util.LanguageMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrowdinProjectCacheProvider {

    private CrowdinProjectCache crowdinProjectCache = new CrowdinProjectCache();

    private boolean outdated = false;

    private final List<String> outdatedBranches = new ArrayList<>();

    public CrowdinProjectCache getCrowdinProjectCache() {
        return crowdinProjectCache;
    }

    public void setCrowdinProjectCache(CrowdinProjectCache crowdinProjectCache) {
        this.crowdinProjectCache = crowdinProjectCache;
    }

    public List<String> getOutdatedBranches() {
        return outdatedBranches;
    }

    public static class CrowdinProjectCache {
        private boolean managerAccess;
        private Project project;
        private List<Language> SupportedLanguages;
        private List<Language> ProjectLanguages;
        private Map<String, Branch> branches;
        private Map<Branch, Map<String, Directory>> dirs;
        private Map<Branch, Map<String, ? extends FileInfo>> fileInfos;
        private LanguageMapping languageMapping;
        private List<SourceString> strings;
        private List<Bundle> bundles;

        public boolean isStringsBased() {
            return Type.STRINGS_BASED == this.project.getType();
        }

        /**
         * Returns project information with additional information. Should be checked for managerAccess before accessing this value
         *
         * @return Project information with additional information
         */
        public ProjectSettings getProjectSettings() {
            this.checkForManagerAccess();
            return (ProjectSettings) this.getProject();
        }

        @SuppressWarnings("unchecked")
        public Map<String, FileInfo> getFileInfos(Branch branch) {
            if (fileInfos.containsKey(branch)) {
                return (Map<String, FileInfo>) fileInfos.get(branch);
            } else {
                Map<String, FileInfo> newMap = new HashMap<>();
                fileInfos.put(branch, newMap);
                return newMap;
            }
        }

        /**
         * Returns list of files with additional information. Should be checked for managerAccess before accessing this values
         *
         * @return List of files with additional information
         */
        @SuppressWarnings("unchecked")
        public Map<String, File> getFiles(Branch branch) {
            this.checkForManagerAccess();
            if (fileInfos.containsKey(branch)) {
                return (Map<String, File>) fileInfos.get(branch);
            } else {
                Map<String, File> newMap = new HashMap<>();
                fileInfos.put(branch, newMap);
                return newMap;
            }
        }

        /**
         * Returns server language mapping. Should be checked for managerAccess before accessing this value
         *
         * @return Langauge Mapping from server
         */
        public LanguageMapping getLanguageMapping() {
            this.checkForManagerAccess();
            return languageMapping;
        }

        private void checkForManagerAccess() {
            if (!isManagerAccess()) {
                throw new RuntimeException("Unexpected error: Manager access is required");
            }
        }

        public boolean isManagerAccess() {
            return managerAccess;
        }

        public void setManagerAccess(boolean managerAccess) {
            this.managerAccess = managerAccess;
        }

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public List<Language> getSupportedLanguages() {
            return SupportedLanguages;
        }

        public void setSupportedLanguages(List<Language> supportedLanguages) {
            SupportedLanguages = supportedLanguages;
        }

        public List<Language> getProjectLanguages() {
            return ProjectLanguages;
        }

        public void setProjectLanguages(List<Language> projectLanguages) {
            ProjectLanguages = projectLanguages;
        }

        public Map<String, Branch> getBranches() {
            return branches;
        }

        public void setBranches(Map<String, Branch> branches) {
            this.branches = branches;
        }

        public Map<Branch, Map<String, Directory>> getDirs() {
            return dirs;
        }

        public void setDirs(Map<Branch, Map<String, Directory>> dirs) {
            this.dirs = dirs;
        }

        public Map<Branch, Map<String, ? extends FileInfo>> getFileInfos() {
            return fileInfos;
        }

        public void setFileInfos(Map<Branch, Map<String, ? extends FileInfo>> fileInfos) {
            this.fileInfos = fileInfos;
        }

        public void setLanguageMapping(LanguageMapping languageMapping) {
            this.languageMapping = languageMapping;
        }

        public List<SourceString> getStrings() {
            return strings;
        }

        public void setStrings(List<SourceString> strings) {
            this.strings = strings;
        }

        public List<Bundle> getBundles() {
            return bundles;
        }

        public void setBundles(List<Bundle> bundles) {
            this.bundles = bundles;
        }
    }

    public synchronized CrowdinProjectCache getInstance(CrowdinClient crowdin, String branchName, boolean update) {
        if (crowdinProjectCache.getProject() == null || update) {
            crowdinProjectCache.setProject(crowdin.getProject());
            crowdinProjectCache.setManagerAccess(crowdinProjectCache.getProject() instanceof ProjectSettings);
            if (crowdinProjectCache.isManagerAccess()) {
                crowdinProjectCache.setLanguageMapping(
                        LanguageMapping.fromServerLanguageMapping(crowdinProjectCache.getProjectSettings().getLanguageMapping()));
            }
        }
        if (crowdinProjectCache.getStrings() == null) {
            crowdinProjectCache.setStrings(crowdin.getStrings());
        }
        if (crowdinProjectCache.getSupportedLanguages() == null) {
            crowdinProjectCache.setSupportedLanguages(crowdin.getSupportedLanguages());
        }
        if (crowdinProjectCache.getProjectLanguages() == null || update) {
            crowdinProjectCache.setProjectLanguages(crowdin.extractProjectLanguages(crowdinProjectCache.getProject()));
        }
        if (crowdinProjectCache.getBundles() == null && crowdinProjectCache.isStringsBased()) {
            crowdinProjectCache.setBundles(crowdin.getBundles());
        }
        if (crowdinProjectCache.getBranches() == null || outdated || update) {
            crowdinProjectCache.setBranches(crowdin.getBranches());
            outdated = false;
        }
        if (crowdinProjectCache.getFileInfos() == null) {
            crowdinProjectCache.setFileInfos(new HashMap<>());
        }
        if (crowdinProjectCache.getDirs() == null) {
            crowdinProjectCache.setDirs(new HashMap<>());
        }
        if ((branchName != null && !branchName.isEmpty()) && !crowdinProjectCache.getBranches().containsKey(branchName)) {
            return crowdinProjectCache;
        }
        Branch branch = crowdinProjectCache.getBranches().get(branchName);
        if (!crowdinProjectCache.getFileInfos().containsKey(branch)
                || !crowdinProjectCache.getDirs().containsKey(branch)
                || outdatedBranches.contains(branchName)
                || update) {
            Long branchId = (branch != null) ? branch.getId() : null;
            if (!crowdinProjectCache.isStringsBased()) {
                List<FileInfo> files = crowdin.getFiles(branchId);
                Map<Long, Directory> dirs = crowdin.getDirectories(branchId);
                crowdinProjectCache.getFileInfos().put(branch, CrowdinFileUtil.buildFilePaths(files, dirs));
                crowdinProjectCache.getDirs().put(branch, CrowdinFileUtil.buildDirPaths(dirs));
            }
            outdatedBranches.remove(branchName);
        }
        return crowdinProjectCache;
    }

    public synchronized void outdateBranch(String branchName) {
        outdated = true;
        outdatedBranches.add(branchName);
    }
}
