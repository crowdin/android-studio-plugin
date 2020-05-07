package com.crowdin.util;

import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

public final class GitUtil {

    private GitUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getCurrentBranch(@NotNull final Project project) {
        String disableBranches = PropertyUtil.getPropertyValue(PropertyUtil.PROPERTY_DISABLE_BRANCHES);

        if (disableBranches != null && disableBranches.equals("true")) {
            return "";
        }

        GitRepository repository;
        GitLocalBranch localBranch;
        String branchName = "";
        try {
            repository = GitBranchUtil.getCurrentRepository(project);
            localBranch = repository.getCurrentBranch();
            branchName = localBranch.getName();
        } catch (Exception e) {
            e.getMessage();
        }
        return branchName;
    }
}
