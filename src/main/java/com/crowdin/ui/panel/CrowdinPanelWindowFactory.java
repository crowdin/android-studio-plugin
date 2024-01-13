package com.crowdin.ui.panel;

import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CrowdinPanelWindowFactory implements ToolWindowFactory, DumbAware {

    private static final String PROGRESS_TOOLBAR_ID = "Crowdin.TranslationProgressToolbar";
    private static final String UPLOAD_TOOLBAR_ID = "Crowdin.UploadToolbar";
    private static final String DOWNLOAD_TOOLBAR_ID = "Crowdin.DownloadToolbar";
    private static final String PROGRESS_REFRESH_ACTION = "Crowdin.RefreshTranslationProgressAction";
    private static final String UPLOAD_REFRESH_ACTION = "Crowdin.RefreshUploadAction";
    private static final String DOWNLOAD_REFRESH_ACTION = "Crowdin.RefreshDownloadAction";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        ActionManager actionManager = ActionManager.getInstance();
        ProjectService projectService = ServiceManager.getService(project, ProjectService.class);

        Content progressPanel = this.setupPanel(
                () -> {
                    TranslationProgressWindow translationProgressWindow = new TranslationProgressWindow();
                    projectService.setTranslationProgressWindow(translationProgressWindow);
                    return translationProgressWindow;
                },
                actionManager,
                contentFactory,
                "Translation Progress",
                PROGRESS_TOOLBAR_ID
        );

        Content uploadPanel = this.setupPanel(
                () -> {
                    UploadWindow uploadWindow = new UploadWindow();
                    projectService.setUploadWindow(uploadWindow);
                    return uploadWindow;
                },
                actionManager,
                contentFactory,
                "Upload",
                UPLOAD_TOOLBAR_ID
        );

        Content downloadPanel = this.setupPanel(
                () -> {
                    DownloadWindow downloadWindow = new DownloadWindow();
                    projectService.setDownloadWindow(downloadWindow);
                    return downloadWindow;
                },
                actionManager,
                contentFactory,
                "Download",
                DOWNLOAD_TOOLBAR_ID
        );

        toolWindow.getContentManager().addContent(progressPanel, 0);
        toolWindow.getContentManager().addContent(uploadPanel, 1);
        toolWindow.getContentManager().addContent(downloadPanel, 2);

        //refresh
        this.runRefresh(progressPanel, actionManager, PROGRESS_REFRESH_ACTION, projectService.getTranslationProgressWindow(), () -> projectService.getTranslationProgressWindow().setPlug("Loading..."));
        this.runRefresh(uploadPanel, actionManager, UPLOAD_REFRESH_ACTION, projectService.getUploadWindow());
        this.runRefresh(downloadPanel, actionManager, DOWNLOAD_REFRESH_ACTION, projectService.getDownloadWindow());
    }

    private void runRefresh(Content panel, ActionManager actionManager, String action, ContentTab contentTab) {
        this.runRefresh(panel, actionManager, action, contentTab, null);
    }

    private void runRefresh(Content panel, ActionManager actionManager, String action, ContentTab contentTab, Runnable onRefresh) {
        DataContext dataContext = DataManager.getInstance().getDataContext(panel.getComponent());
        if (dataContext.getData(CommonDataKeys.PROJECT) != null) {
            AnAction refreshDownloadAction = actionManager.getAction(action);
            refreshDownloadAction.actionPerformed(new AnActionEvent(
                    null, DataManager.getInstance().getDataContext(contentTab.getContent()),
                    ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
            if (onRefresh != null) {
                onRefresh.run();
            }
        }
    }

    private Content setupPanel(
            Supplier<ContentTab> tabSupplier,
            ActionManager actionManager,
            ContentFactory contentFactory,
            String name,
            String actionId
    ) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        ContentTab contentTab = tabSupplier.get();

        panel.setContent(contentTab.getContent());

        ActionGroup group = (ActionGroup) actionManager.getAction(actionId);

        ActionToolbar toolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, group, true);
        panel.setToolbar(toolbar.getComponent());

        return contentFactory.createContent(panel, name, false);
    }

    public static class ProjectService {

        private TranslationProgressWindow translationProgressWindow;
        private UploadWindow uploadWindow;
        private DownloadWindow downloadWindow;

        public void setTranslationProgressWindow(TranslationProgressWindow translationProgressWindow) {
            this.translationProgressWindow = translationProgressWindow;
        }

        public TranslationProgressWindow getTranslationProgressWindow() {
            return translationProgressWindow;
        }

        public UploadWindow getUploadWindow() {
            return uploadWindow;
        }

        public void setUploadWindow(UploadWindow uploadWindow) {
            this.uploadWindow = uploadWindow;
        }

        public DownloadWindow getDownloadWindow() {
            return downloadWindow;
        }

        public void setDownloadWindow(DownloadWindow downloadWindow) {
            this.downloadWindow = downloadWindow;
        }
    }

}
