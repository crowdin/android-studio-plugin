package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.sourcefiles.model.Branch;
import com.intellij.openapi.vfs.VirtualFile;

public class ActionContext {

    public final String branchName;
    public final Branch branch;
    public final VirtualFile root;
    public final CrowdinProperties properties;
    public final Crowdin crowdin;
    public final CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache;

    public ActionContext(String branchName, Branch branch, VirtualFile root, CrowdinProperties properties, Crowdin crowdin, CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache) {
        this.branchName = branchName;
        this.branch = branch;
        this.root = root;
        this.properties = properties;
        this.crowdin = crowdin;
        this.crowdinProjectCache = crowdinProjectCache;
    }
}
