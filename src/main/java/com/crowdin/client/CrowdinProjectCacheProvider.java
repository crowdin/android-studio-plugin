package com.crowdin.client;

import com.crowdin.client.languages.model.Language;
import com.crowdin.client.projectsgroups.model.Project;
import com.crowdin.client.projectsgroups.model.ProjectSettings;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.util.CrowdinFileUtil;
import com.crowdin.util.LanguageMapping;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrowdinProjectCacheProvider {

    private static CrowdinProjectCache crowdinProjectCache;

    private static boolean outdated = false;
    private static List<String> outdatedBranches = new ArrayList<>();

    @Data
    public static class CrowdinProjectCache {
        private boolean managerAccess;
        private Project project;
        private List<Language> ProjectLanguages;
        private Map<String, Branch> branches;
        private Map<Branch, Map<String, Directory>> dirs;
        private Map<Branch, Map<String, ? extends FileInfo>> fileInfos;
        private LanguageMapping languageMapping;

        /**
         * Returns project information with additional information. Should be checked for managerAccess before accessing this value
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

    }

    private CrowdinProjectCacheProvider() {

    }

    public synchronized static CrowdinProjectCache getInstance(Crowdin crowdin, String branchName, boolean update) {
        if (crowdinProjectCache == null) {
            crowdinProjectCache = new CrowdinProjectCache();
        }
        if (crowdinProjectCache.getProject() == null || update) {
            crowdinProjectCache.setProject(crowdin.getProject());
            crowdinProjectCache.setManagerAccess(crowdinProjectCache.getProject() instanceof ProjectSettings);
            if (crowdinProjectCache.isManagerAccess()) {
                crowdinProjectCache.setLanguageMapping(
                    LanguageMapping.fromServerLanguageMapping(crowdinProjectCache.getProjectSettings().getLanguageMapping()));
            }
        }
        if (crowdinProjectCache.getProjectLanguages() == null || update) {
            crowdinProjectCache.setProjectLanguages(crowdin.extractProjectLanguages(crowdinProjectCache.getProject()));
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
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!! here");
        if (!crowdinProjectCache.getFileInfos().containsKey(branch)
                || !crowdinProjectCache.getDirs().containsKey(branch)
                || outdatedBranches.contains(branchName)
                || update) {
            Long branchId = (branch != null) ? branch.getId() : null;
            List<com.crowdin.client.sourcefiles.model.FileInfo> files = crowdin.getFiles(branchId);
            Map<Long, Directory> dirs = crowdin.getDirectories(branchId);
            System.out.println("!!!!!!!!!!!!!!!!! updating files: " + files.size());
            crowdinProjectCache.getFileInfos().put(branch, CrowdinFileUtil.buildFilePaths(files, dirs));
            System.out.println("crowdinProjectCache.getFileInfos() = " + crowdinProjectCache.getFileInfos());
            crowdinProjectCache.getDirs().put(branch, CrowdinFileUtil.buildDirPaths(dirs));
            outdatedBranches.remove(branchName);
        }
        return crowdinProjectCache;
    }

    public synchronized static void outdateBranch(String branchName) {
        outdated = true;
        outdatedBranches.add(branchName);
    }
}
