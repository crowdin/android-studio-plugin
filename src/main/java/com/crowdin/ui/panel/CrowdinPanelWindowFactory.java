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

        Content progressPanel = this.setupProgressPanel(actionManager, contentFactory, projectService);

        Content uploadPanel = this.setupUploadPanel(actionManager, contentFactory, projectService);

        Content downloadPanel = this.setupDownloadPanel(actionManager, contentFactory, projectService);

        toolWindow.getContentManager().addContent(progressPanel, 0);
        toolWindow.getContentManager().addContent(uploadPanel, 1);
        toolWindow.getContentManager().addContent(downloadPanel, 2);

        //refresh
        DataContext dataContext = DataManager.getInstance().getDataContext(progressPanel.getComponent());
        if (dataContext.getData(CommonDataKeys.PROJECT) != null) {
            //refresh progress
            TranslationProgressWindow translationProgressWindow = projectService.getTranslationProgressWindow();
            AnAction refreshAction = actionManager.getAction(PROGRESS_REFRESH_ACTION);
            refreshAction.actionPerformed(new AnActionEvent(
                    null, DataManager.getInstance().getDataContext(translationProgressWindow.getContent()),
                    ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
            translationProgressWindow.setPlug("Loading...");
            //refresh upload
            UploadWindow uploadWindow = projectService.getUploadWindow();
            AnAction refreshUploadAction = actionManager.getAction(UPLOAD_REFRESH_ACTION);
            refreshUploadAction.actionPerformed(new AnActionEvent(
                    null, DataManager.getInstance().getDataContext(uploadWindow.getContent()),
                    ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
            //refresh download
            DownloadWindow downloadWindow = projectService.getDownloadWindow();
            AnAction refreshDownloadAction = actionManager.getAction(DOWNLOAD_REFRESH_ACTION);
            refreshDownloadAction.actionPerformed(new AnActionEvent(
                    null, DataManager.getInstance().getDataContext(downloadWindow.getContent()),
                    ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));

        }
    }

    private Content setupProgressPanel(ActionManager actionManager, ContentFactory contentFactory, ProjectService projectService) {
        SimpleToolWindowPanel progressPanel = new SimpleToolWindowPanel(true, true);
        TranslationProgressWindow translationProgressWindow = new TranslationProgressWindow();

        projectService.setTranslationProgressWindow(translationProgressWindow);

        progressPanel.setContent(translationProgressWindow.getContent());

        ActionGroup group = (ActionGroup) actionManager.getAction(PROGRESS_TOOLBAR_ID);

        ActionToolbar toolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, group, true);
        progressPanel.setToolbar(toolbar.getComponent());

        return contentFactory.createContent(progressPanel, "Translation Progress", false);
    }

    private Content setupDownloadPanel(ActionManager actionManager, ContentFactory contentFactory, ProjectService projectService) {
        SimpleToolWindowPanel downloadPanel = new SimpleToolWindowPanel(true, true);

        DownloadWindow downloadWindow = new DownloadWindow();

        projectService.setDownloadWindow(downloadWindow);

        downloadPanel.setContent(downloadWindow.getContent());

        ActionGroup group = (ActionGroup) actionManager.getAction(DOWNLOAD_TOOLBAR_ID);

        ActionToolbar toolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, group, true);
        downloadPanel.setToolbar(toolbar.getComponent());

        return contentFactory.createContent(downloadPanel, "Download", false);
    }

    private Content setupUploadPanel(ActionManager actionManager, ContentFactory contentFactory, ProjectService projectService) {
        SimpleToolWindowPanel uploadPanel = new SimpleToolWindowPanel(true, true);

        UploadWindow uploadWindow = new UploadWindow();

        projectService.setUploadWindow(uploadWindow);

        uploadPanel.setContent(uploadWindow.getContent());

        ActionGroup group = (ActionGroup) actionManager.getAction(UPLOAD_TOOLBAR_ID);

        ActionToolbar toolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, group, true);
        uploadPanel.setToolbar(toolbar.getComponent());

        return contentFactory.createContent(uploadPanel, "Upload", false);
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
