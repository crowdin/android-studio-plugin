package com.crowdin.util;

import com.crowdin.client.BranchInfo;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public final class GitUtil {

    private static final Set<Character> BRANCH_UNALLOWED_SYMBOLS = new HashSet<>(
            Arrays.asList('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    );

    private GitUtil() {
        throw new UnsupportedOperationException();
    }

    public static BranchInfo getCurrentBranch(@NotNull final Project project) {
        GitRepository repository;
        GitLocalBranch localBranch;
        String branchName = "";
        try {
            repository = GitBranchUtil.guessWidgetRepository(project, FileUtil.getProjectBaseDir(project));
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
        return new BranchInfo(normalizeBranchName(branchName), branchName);
    }

    public static String normalizeBranchName(String branch) {
        StringBuilder res = new StringBuilder();
        for (char character : branch.toCharArray()) {
            if (BRANCH_UNALLOWED_SYMBOLS.contains(character)) {
                res.append(".");
            } else {
                res.append(character);
            }
        }
        return res.toString();
    }
}
