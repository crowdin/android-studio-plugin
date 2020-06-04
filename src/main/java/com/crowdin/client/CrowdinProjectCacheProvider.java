package com.crowdin.client;

import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.util.CrowdinFileUtil;
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
        private List<Language> ProjectLanguages;
        private Map<String, Branch> branches;
        private Map<Branch, Map<String, File>> files;
    }

    private CrowdinProjectCacheProvider() {

    }

    public synchronized static CrowdinProjectCache getInstance(Crowdin crowdin, String branchName, boolean update) {
        if (crowdinProjectCache == null) {
            crowdinProjectCache = new CrowdinProjectCache();
        }
        if (crowdinProjectCache.getProjectLanguages() == null || update) {
            crowdinProjectCache.setProjectLanguages(crowdin.getProjectLanguages());
        }
        if (crowdinProjectCache.getBranches() == null || outdated || update) {
            crowdinProjectCache.setBranches(crowdin.getBranches());
            outdated = false;
        }
        if (crowdinProjectCache.getFiles() == null) {
            crowdinProjectCache.setFiles(new HashMap<>());
        }
        if (!crowdinProjectCache.getBranches().containsKey(branchName)) {
            return crowdinProjectCache;
        }
        Branch branch = crowdinProjectCache.getBranches().get(branchName);
        if (!crowdinProjectCache.getFiles().containsKey(branch) || outdatedBranches.contains(branchName) || update) {
            List<com.crowdin.client.sourcefiles.model.File> files = crowdin.getFiles(branch.getId());
            Map<Long, Directory> dirs = crowdin.getDirectories(branch.getId());
            crowdinProjectCache.getFiles().put(branch, CrowdinFileUtil.buildFilePaths(files, dirs));
            outdatedBranches.remove(branchName);
        }
        return crowdinProjectCache;
    }

    public synchronized static void outdateBranch(String branchName) {
        outdated = true;
        outdatedBranches.add(branchName);
    }
}
