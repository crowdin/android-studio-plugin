package com.crowdin.ui.panel;

import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.ui.panel.upload.UploadWindow;
import com.intellij.ide.ActivityTracker;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
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
        ContentFactory contentFactory = ContentFactory.getInstance();
        ContentManager contentManager = toolWindow.getContentManager();
        ActionManager actionManager = ActionManager.getInstance();
        ProjectService projectService = project.getService(ProjectService.class);

        Content progressPanel = this.setupPanel(
                () -> {
                    TranslationProgressWindow translationProgressWindow = new TranslationProgressWindow();
                    projectService.setTranslationProgressWindow(translationProgressWindow);
                    return translationProgressWindow;
                },
                (ActionGroup) actionManager.getAction(PROGRESS_TOOLBAR_ID),
                contentFactory,
                "Progress"
        );

        Content uploadPanel = this.setupPanel(
                () -> {
                    UploadWindow uploadWindow = new UploadWindow();
                    projectService.setUploadWindow(uploadWindow);
                    return uploadWindow;
                },
                (ActionGroup) actionManager.getAction(UPLOAD_TOOLBAR_ID),
                contentFactory,
                "Upload"
        );

        Content downloadPanel = this.setupPanel(
                () -> {
                    DownloadWindow downloadWindow = new DownloadWindow();
                    projectService.setDownloadWindow(downloadWindow);
                    return downloadWindow;
                },
                (ActionGroup) actionManager.getAction(DOWNLOAD_TOOLBAR_ID),
                contentFactory,
                "Download"
        );

        contentManager.addContent(uploadPanel, 0);
        contentManager.addContent(downloadPanel, 1);
        contentManager.addContent(progressPanel, 2);

        EnumSet<ProjectService.InitializationItem> loadedComponents = projectService.addAndGetLoadedComponents(ProjectService.InitializationItem.UI_PANELS);
        if (loadedComponents.contains(ProjectService.InitializationItem.STARTUP_ACTIVITY)) {
            reloadPanels(project, true);
        }
    }

    private Content setupPanel(
            Supplier<ContentTab> tabSupplier,
            ActionGroup group,
            ContentFactory contentFactory,
            String name
    ) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        ActionToolbarImpl actionToolbar = new ActionToolbarImpl(ActionPlaces.TOOLBAR, group, true);

        actionToolbar.setTargetComponent(panel);

        panel.setToolbar(actionToolbar);
        panel.setContent(tabSupplier.get().getContent());

        return contentFactory.createContent(panel, name, false);
    }

    public static void reloadPanels(Project project, boolean fullReload) {
        Optional
                .ofNullable(ToolWindowManager.getInstance(project))
                .map(toolWindowManager -> toolWindowManager.getToolWindow(TOOLWINDOW_ID))
                .map(ToolWindow::getContentManager)
                .ifPresent(manager -> {
                    ActionManager actionManager = ActionManager.getInstance();
                    ProjectService projectService = project.getService(ProjectService.class);
                    if (fullReload) {
                        runRefresh(project, actionManager, PROGRESS_REFRESH_ACTION, () -> projectService.getTranslationProgressWindow().setPlug("Loading..."));
                    }
                    runRefresh(project, actionManager, UPLOAD_REFRESH_ACTION);
                    runRefresh(project, actionManager, DOWNLOAD_REFRESH_ACTION);
                });
    }

    public static void updateToolbar(String actionId, String text, boolean visible, boolean enabled) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        Presentation presentation = new Presentation();
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
        presentation.setText(text);
        action.update(AnActionEvent.createFromDataContext(TOOLWINDOW_ID, presentation, DataContext.EMPTY_CONTEXT));
        ActivityTracker.getInstance().inc();
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

}
