package com.crowdin.util;

import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public final class GitUtil {

    private GitUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getCurrentBranch(@NotNull final Project project) {
        GitRepository repository;
        GitLocalBranch localBranch;
        String branchName = "";
        try {
            repository = GitBranchUtil.getCurrentRepository(project);
            if (repository == null) {
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.not_found_git_branch"));
            }
            localBranch = repository.getCurrentBranch();
            if (localBranch == null) {
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.not_found_git_branch"));
            }
            branchName = localBranch.getName();
        } catch (Exception e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.get_git_branch_name"), e.getMessage()), e);
        }
        return branchName;
    }
}
