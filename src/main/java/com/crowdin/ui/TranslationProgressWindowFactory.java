package com.crowdin.ui;

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

public class TranslationProgressWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);

        TranslationProgressWindow translationProgressWindow = new TranslationProgressWindow();

        ProjectService projectService = ServiceManager.getService(project, ProjectService.class);
        projectService.setTranslationProgressWindow(translationProgressWindow);

        panel.setContent(translationProgressWindow.getContent());

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel, "Translation Progress", false);
        toolWindow.getContentManager().addContent(content);

        ActionManager actionManager = ActionManager.getInstance();

        ActionGroup group = (ActionGroup) actionManager.getAction("Crowdin.TranslationProgressToolbar");

        ActionToolbar toolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, group, true);
        panel.setToolbar(toolbar.getComponent());

        DataContext dataContext = DataManager.getInstance().getDataContext(content.getComponent());
        if (dataContext.getData(CommonDataKeys.PROJECT) != null) {
            AnAction refreshAction = actionManager.getAction("Crowdin.RefreshTranslationProgressAction");
            refreshAction.actionPerformed(new AnActionEvent(
                null, DataManager.getInstance().getDataContext(translationProgressWindow.getContent()),
                ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
            translationProgressWindow.setPlug("Loading...");
        }
    }

    public static class ProjectService {

        private TranslationProgressWindow translationProgressWindow;

        public void setTranslationProgressWindow(TranslationProgressWindow translationProgressWindow) {
            this.translationProgressWindow = translationProgressWindow;
        }

        public TranslationProgressWindow getTranslationProgressWindow() {
            return translationProgressWindow;
        }
    }

}
