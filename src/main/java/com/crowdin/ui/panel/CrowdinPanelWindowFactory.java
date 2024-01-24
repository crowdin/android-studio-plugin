package com.crowdin.ui.panel;

import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

import static com.crowdin.Constants.DOWNLOAD_REFRESH_ACTION;
import static com.crowdin.Constants.DOWNLOAD_TOOLBAR_ID;
import static com.crowdin.Constants.PROGRESS_REFRESH_ACTION;
import static com.crowdin.Constants.PROGRESS_TOOLBAR_ID;
import static com.crowdin.Constants.TOOLWINDOW_ID;
import static com.crowdin.Constants.UPLOAD_REFRESH_ACTION;
import static com.crowdin.Constants.UPLOAD_TOOLBAR_ID;

public class CrowdinPanelWindowFactory implements ToolWindowFactory, DumbAware {

    public static final String PLACE_ID = CrowdinPanelWindowFactory.class.getName();

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
    }

    public static void reloadPanels(Project project, boolean fullReload) {
        Optional
                .ofNullable(ToolWindowManager.getInstance(project))
                .map(toolWindowManager -> toolWindowManager.getToolWindow(TOOLWINDOW_ID))
                .map(ToolWindow::getContentManager)
                .ifPresent(manager -> {
                    ActionManager actionManager = ActionManager.getInstance();
                    ProjectService projectService = ServiceManager.getService(project, ProjectService.class);
                    if (fullReload) {
                        runRefresh(project, actionManager, PROGRESS_REFRESH_ACTION, () -> projectService.getTranslationProgressWindow().setPlug("Loading..."));
                    }
                    runRefresh(project, actionManager, UPLOAD_REFRESH_ACTION);
                    runRefresh(project, actionManager, DOWNLOAD_REFRESH_ACTION);
                });
    }

    private static void runRefresh(Project project, ActionManager actionManager, String action) {
        runRefresh(project, actionManager, action, null);
    }

    private static void runRefresh(Project project, ActionManager actionManager, String action, Runnable onRefresh) {
        AnAction refreshAction = actionManager.getAction(action);
        DataContext context = dataId -> project;
        AnActionEvent anActionEvent = new AnActionEvent(
                null,
                context,
                PLACE_ID,
                new Presentation(),
                actionManager,
                0
        );
        refreshAction.actionPerformed(anActionEvent);
        if (onRefresh != null) {
            onRefresh.run();
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
