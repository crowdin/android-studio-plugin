package com.crowdin.action;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.CrowdinSettings;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.StringUtils;
import com.crowdin.util.UIUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public abstract class BackgroundAction extends AnAction {

    public BackgroundAction() {
    }

    public BackgroundAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }

    protected abstract void performInBackground(@NotNull AnActionEvent e, @NotNull ProgressIndicator indicator);

    protected abstract String loadingText(AnActionEvent e);

    protected Optional<ActionContext> prepare(
            Project project,
            ProgressIndicator indicator,
            boolean checkForManagerAccess,
            boolean createBranchIfNotExists,
            boolean realodCrowdinCache,
            String question,
            String okBtn
    ) {
        VirtualFile root = FileUtil.getProjectBaseDir(project);

        CrowdinSettings crowdinSettings = ServiceManager.getService(project, CrowdinSettings.class);

        if (!StringUtils.isEmpty(question) && !StringUtils.isEmpty(okBtn)) {
            boolean confirmation = UIUtil.confirmDialog(project, crowdinSettings, MESSAGES_BUNDLE.getString(question), okBtn);
            if (!confirmation) {
                return Optional.empty();
            }
            if (indicator != null) {
                indicator.checkCanceled();
            }
        }

        CrowdinProperties properties;
        try {
            properties = CrowdinPropertiesLoader.load(project);
        } catch (Exception e) {
            NotificationUtil.showErrorMessage(project, e.getMessage());
            return Optional.empty();
        }
        NotificationUtil.setLogDebugLevel(properties.isDebug());
        NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.started_action"));

        Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

        BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
        String branchName = branchLogic.acquireBranchName();
        if (indicator != null) {
            indicator.checkCanceled();
        }

        CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                CrowdinProjectCacheProvider.getInstance(crowdin, branchName, realodCrowdinCache);

        if (checkForManagerAccess) {
            if (!crowdinProjectCache.isManagerAccess()) {
                NotificationUtil.showErrorMessage(project, "You need to have manager access to perform this action");
                return Optional.empty();
            }
        }

        Branch branch = branchLogic.getBranch(crowdinProjectCache, createBranchIfNotExists);

        if (indicator != null) {
            indicator.checkCanceled();
        }

        return Optional.of(new ActionContext(branchName, branch, root, properties, crowdin, crowdinProjectCache));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ProgressManager.getInstance().run(new Task.Backgroundable(e.getProject(), "Crowdin") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText(loadingText(e));
                performInBackground(e, indicator);
            }
        });
    }
}
