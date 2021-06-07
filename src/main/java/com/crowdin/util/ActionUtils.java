package com.crowdin.util;

import com.crowdin.client.CrowdinProperties;
import com.intellij.openapi.project.Project;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class ActionUtils {

    public static String getBranchName(Project project, CrowdinProperties properties, boolean performCheck) {
        String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);
        if (performCheck) {
            if (!CrowdinFileUtil.isValidBranchName(branchName)) {
                throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.branch_contains_forbidden_symbols"));
            }
        }
        return branchName;
    }
}
