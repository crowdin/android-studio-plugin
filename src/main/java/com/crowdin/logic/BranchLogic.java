package com.crowdin.logic;

import com.crowdin.client.BranchInfo;
import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.util.CrowdinFileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.StringUtils;
import com.intellij.openapi.project.Project;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class BranchLogic {

    private final Crowdin crowdin;
    private final Project project;
    private final CrowdinProperties properties;

    private BranchInfo branchInfo;

    public BranchLogic(Crowdin crowdin, Project project, CrowdinProperties properties) {
        this.crowdin = crowdin;
        this.project = project;
        this.properties = properties;
    }

    public String acquireBranchName() {
        return this.acquireBranchName(false);
    }

    public String acquireBranchName(boolean performCheck) {
        BranchInfo branch;
        if (properties.isDisabledBranches()) {
            branch = new BranchInfo("", "");
        } else {
            branch = GitUtil.getCurrentBranch(project);
        }
        if (performCheck) {
            if (!CrowdinFileUtil.isValidBranchName(branch.getName())) {
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.branch_contains_forbidden_symbols"));
            }
        }
        this.branchInfo = branch;
        return branch.getName();
    }

    public Branch getBranch(CrowdinProjectCacheProvider.CrowdinProjectCache projectCache, boolean createIfNotExists) {
        if (this.branchInfo == null) {
            this.acquireBranchName();
        }
        Branch branch = projectCache.getBranches().get(this.branchInfo.getName());
        if (branch == null && !StringUtils.isEmpty(this.branchInfo.getName())) {
            if (createIfNotExists) {
                AddBranchRequest addBranchRequest = RequestBuilder.addBranch(this.branchInfo.getName(), this.branchInfo.getTitle());
                branch = crowdin.addBranch(addBranchRequest);
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.created_branch"), branch.getId(), branch.getName()));
            } else {
                throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.branch_not_exists"), this.branchInfo.getName()));
            }
        } else if (branch != null) {
            NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.using_branch"), branch.getId(), branch.getName()));
        }
        return branch;
    }
}
