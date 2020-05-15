package com.crowdin.event;

import com.crowdin.client.Crowdin;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.crowdin.util.FileUtil.PARENT_FOLDER_NAME;
import static com.crowdin.util.PropertyUtil.PROPERTY_SOURCES;

/**
 * Created by ihor on 1/20/17.
 */
public class FileChangeListener implements ApplicationComponent, BulkFileListener {

    private static final String PROPERTY_AUTO_UPLOAD = "auto-upload";

    private final MessageBusConnection connection;
    private final Project project;

    @NotNull
    @Override
    public String getComponentName() {
        return "Crowdin";
    }

    public FileChangeListener(Project project) {
        this.project = project;
        connection = ApplicationManager.getApplication().getMessageBus().connect();
    }

    public void initComponent() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    public void disposeComponent() {
        connection.disconnect();
    }

    public void before(List<? extends VFileEvent> events) {
    }

    public void after(List<? extends VFileEvent> events) {
        if (this.autoUploadOff()) {
            return;
        }
        String sources = PropertyUtil.getPropertyValue(PROPERTY_SOURCES, this.project);
        List<String> sourcesList = FileUtil.getSourcesList(sources);
        for (VFileEvent e : events) {
            VirtualFile virtualFile = e.getFile();
            if (virtualFile != null && sourcesList.contains(virtualFile.getName()) && PARENT_FOLDER_NAME.equals(virtualFile.getParent().getName())) {
                Project[] projects = ProjectManager.getInstance().getOpenProjects();
                Project project = null;
                if (projects.length == 1) {
                    project = projects[0];
                }
                System.out.println("Changed file " + virtualFile.getCanonicalPath());
                Crowdin crowdin = new Crowdin(this.project);
                String branch = GitUtil.getCurrentBranch(project);
                crowdin.uploadFile(virtualFile, branch);
            }
        }
    }

    private boolean autoUploadOff() {
        String autoUploadProp = PropertyUtil.getPropertyValue(PROPERTY_AUTO_UPLOAD, this.project);
        return autoUploadProp != null && autoUploadProp.equals("false");
    }
}
