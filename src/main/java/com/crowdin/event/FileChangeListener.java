package com.crowdin.event;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
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
                CrowdinProperties properties;
                try {
                    properties = CrowdinPropertiesLoader.load(project);
                } catch (Exception e) {
                    return;
                }
                String branch = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);
                Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());
                List<VirtualFile> filesList = properties.getSourcesWithPatterns().keySet()
                    .stream()
                    .flatMap(s -> FileUtil.getSourceFilesRec(project.getBaseDir(), s).stream())
                    .collect(Collectors.toList());
                List<VirtualFile> files = events.stream()
                        .map(VFileEvent::getFile)
                        .filter(file -> file != null && filesList.contains(file))
                        .collect(Collectors.toList());
                if (files.size() > 0) {
                    String text = files.stream()
                            .map(VirtualFile::getName)
                            .collect(Collectors.joining(",", "Uploading ", " file" + (files.size() == 1 ? "" : "s")));
                    indicator.setText(text);
                    files.forEach(file -> crowdin.uploadFile(file, "/values-%android_code%/%original_file_name%", branch));
                }
            }
        });
    }

    private boolean autoUploadOff() {
        String autoUploadProp = PropertyUtil.getPropertyValue(PROPERTY_AUTO_UPLOAD, this.project);
        return PropertyUtil.getCrowdinPropertyFile(this.project) == null || (autoUploadProp != null && autoUploadProp.equals("false"));
    }

    @Override
    public void dispose() {
        connection.disconnect();
    }
}
