package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.config.CrowdinConfig;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.intellij.openapi.vfs.VirtualFile;

public class ActionContext {

    public final String branchName;
    public final Branch branch;
    public final VirtualFile root;
    public final CrowdinConfig properties;
    public final Crowdin crowdin;
    public final CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache;

    public ActionContext(String branchName, Branch branch, VirtualFile root, CrowdinConfig properties, Crowdin crowdin, CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache) {
        this.branchName = branchName;
        this.branch = branch;
        this.root = root;
        this.properties = properties;
        this.crowdin = crowdin;
        this.crowdinProjectCache = crowdinProjectCache;
    }
}
