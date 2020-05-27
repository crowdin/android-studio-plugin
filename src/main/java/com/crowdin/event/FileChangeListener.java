package com.crowdin.event;

import com.crowdin.client.Crowdin;
import com.crowdin.util.FileUtil;
import com.crowdin.util.GitUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.crowdin.util.FileUtil.PARENT_FOLDER_NAME;
import static com.crowdin.util.PropertyUtil.PROPERTY_SOURCES;

public class FileChangeListener implements Disposable, BulkFileListener {

    private static final String PROPERTY_AUTO_UPLOAD = "auto-upload";

    private final MessageBusConnection connection;
    private final Project project;

    public FileChangeListener(Project project) {
        this.project = project;
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    public void after(List<? extends VFileEvent> events) {
        if (this.project.isDisposed()) {
            return;
        }
        ProjectFileIndex instance = ProjectFileIndex.getInstance(this.project);
        List<? extends VFileEvent> interestedFiles = events.stream()
                .filter(f -> f.getFile() != null && instance.isInContent(f.getFile()))
                .collect(Collectors.toList());
        if (interestedFiles.size() == 0 || this.autoUploadOff()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(this.project, "Crowdin") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String sources = PropertyUtil.getPropertyValue(PROPERTY_SOURCES, project);
                List<String> sourcesList = FileUtil.getSourcesList(sources);
                String branch = GitUtil.getCurrentBranch(project);
                Crowdin crowdin = new Crowdin(project);
                List<VirtualFile> files = events.stream()
                        .map(VFileEvent::getFile)
                        .filter(file -> isSourceFile(file, sourcesList))
                        .collect(Collectors.toList());
                if (files.size() > 0) {
                    String text = files.stream()
                            .map(VirtualFile::getName)
                            .collect(Collectors.joining(",", "Uploading ", " file" + (files.size() == 1 ? "" : "s")));
                    indicator.setText(text);
                    files.forEach(file -> crowdin.uploadFile(file, branch));
                }
            }
        });
    }

    private boolean autoUploadOff() {
        String autoUploadProp = PropertyUtil.getPropertyValue(PROPERTY_AUTO_UPLOAD, this.project);
        return PropertyUtil.getCrowdinPropertyFile(this.project) == null || (autoUploadProp != null && autoUploadProp.equals("false"));
    }

    private boolean isSourceFile(VirtualFile virtualFile, List<String> sourcesList) {
        return virtualFile != null
                && sourcesList.contains(virtualFile.getName())
                && PARENT_FOLDER_NAME.equals(virtualFile.getParent().getName());
    }

    @Override
    public void dispose() {
        connection.disconnect();
    }
}
