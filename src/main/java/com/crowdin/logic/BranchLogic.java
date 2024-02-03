package com.crowdin.logic;

import com.crowdin.client.BranchInfo;
import com.crowdin.client.Crowdin;
import com.crowdin.client.config.CrowdinConfig;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.util.CrowdinFileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.StringUtils;
import com.intellij.openapi.project.Project;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class BranchLogic {

    private final Crowdin crowdin;
    private final Project project;
    private final CrowdinConfig properties;

    private BranchInfo branchInfo;

    public BranchLogic(Crowdin crowdin, Project project, CrowdinConfig properties) {
        this.crowdin = crowdin;
        this.project = project;
        this.properties = properties;
    }

    public String acquireBranchName() {
        BranchInfo branch;
        if (properties.isDisabledBranches()) {
            branch = new BranchInfo("", "");
        } else {
            branch = GitUtil.getCurrentBranch(project);
        }
        if (!CrowdinFileUtil.isValidBranchName(branch.name())) {
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.branch_contains_forbidden_symbols"));
        }
        this.branchInfo = branch;
        return branch.name();
    }

    public Branch getBranch(CrowdinProjectCacheProvider.CrowdinProjectCache projectCache, boolean createIfNotExists) {
        if (this.branchInfo == null) {
            this.acquireBranchName();
        }
        Branch branch = projectCache.getBranches().get(this.branchInfo.name());
        if (branch == null && !StringUtils.isEmpty(this.branchInfo.name())) {
            if (createIfNotExists) {
                AddBranchRequest addBranchRequest = RequestBuilder.addBranch(this.branchInfo.name(), this.branchInfo.title());
                branch = crowdin.addBranch(addBranchRequest);
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.created_branch"), branch.getId(), branch.getName()));
            } else {
                throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.branch_not_exists"), this.branchInfo.name()));
            }
        } else if (branch != null) {
            NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.using_branch"), branch.getId(), branch.getName()));
        }
        return branch;
    }
}
