package com.crowdin.event;

import com.crowdin.command.Crowdin;
import com.crowdin.utils.Utils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by ihor on 1/20/17.
 */
public class FileChangeListener implements ApplicationComponent, BulkFileListener {

    public static final String SOURCE_FILE_DEFAULT = "strings.xml";

    public static final String SOURCE_FOLDER_DEFAULT = "values";

    private final MessageBusConnection connection;

    @NotNull
    @Override
    public String getComponentName() {
        return "Crowdin";
    }

    public FileChangeListener() {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
    }

    public void initComponent() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    public void disposeComponent() {
        connection.disconnect();
    }

    public void before(List<? extends VFileEvent> events) {}

    public void after(List<? extends VFileEvent> events) {
        String sources = Utils.getPropertyValue("sources");
        List<String> sourcesList = Utils.getSourcesList(sources);
        for (VFileEvent e : events) {
            VirtualFile virtualFile = e.getFile();
            if (virtualFile != null && sourcesList.contains(virtualFile.getName()) && SOURCE_FOLDER_DEFAULT.equals(virtualFile.getParent().getName())) {
                Project[] projects = ProjectManager.getInstance().getOpenProjects();
                Project project = null;
                if (projects.length == 1) {
                    project = projects[0];
                }
                System.out.println("Changed file " + virtualFile.getCanonicalPath());
                Crowdin crowdin = new Crowdin();
                String branch = Utils.getCurrentBranch(project);
                crowdin.uploadFile(virtualFile, branch);
            }
        }
    }
}