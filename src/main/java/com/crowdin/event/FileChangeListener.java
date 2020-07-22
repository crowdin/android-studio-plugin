package com.crowdin.event;

import com.crowdin.client.*;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.logic.SourceLogic;
import com.crowdin.util.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;
import static com.crowdin.Constants.PROPERTY_AUTO_UPLOAD;

public class FileChangeListener implements Disposable, BulkFileListener {

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
                try {
                    CrowdinProperties properties;
                    try {
                        properties = CrowdinPropertiesLoader.load(project);
                    } catch (Exception e) {
                        return;
                    }
                    indicator.checkCanceled();
                    String branchName = properties.isDisabledBranches() ? "" : GitUtil.getCurrentBranch(project);
                    if (!CrowdinFileUtil.isValidBranchName(branchName)) {
                        return;
                    }
                    Crowdin crowdin = new Crowdin(project, properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

                    CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                        CrowdinProjectCacheProvider.getInstance(crowdin, branchName, false);
                    indicator.checkCanceled();

                    Map<VirtualFile, Pair<String, String>> allSources = new HashMap<>();
                    properties.getSourcesWithPatterns().forEach((sourcePattern, translationPattern) -> {
                        List<VirtualFile> files = FileUtil.getSourceFilesRec(FileUtil.getProjectBaseDir(project), sourcePattern);
                        files.forEach(f -> allSources.put(f, Pair.create(sourcePattern, translationPattern)));
                    });

                    List<VirtualFile> changedSources = events.stream()
                        .map(VFileEvent::getFile)
                        .filter(file -> file != null && allSources.containsKey(file))
                        .collect(Collectors.toList());

                    if (changedSources.isEmpty()) {
                        return;
                    }

                    Branch branch = crowdinProjectCache.getBranches().get(branchName);
                    if (branch == null && StringUtils.isNotEmpty(branchName)) {
                        AddBranchRequest addBranchRequest = RequestBuilder.addBranch(branchName);
                        branch = crowdin.addBranch(addBranchRequest);
                    }
                    indicator.checkCanceled();

                    Map<String, File> filePaths = crowdinProjectCache.getFiles().getOrDefault(branch, new HashMap<>());
                    Map<String, Directory> dirPaths = crowdinProjectCache.getDirs().getOrDefault(branch, new HashMap<>());
                    Long branchId = (branch != null) ? branch.getId() : null;

                    SourceLogic sourceLogic = new SourceLogic(project, crowdin, properties, filePaths, dirPaths, branchId);

                    indicator.checkCanceled();
                    String text = changedSources.stream()
                        .map(VirtualFile::getName)
                        .collect(Collectors.joining(","));
                    indicator.setText(String.format(MESSAGES_BUNDLE.getString("messages.uploading_file_s"), text, changedSources.size() == 1 ? "" : "s"));
                    changedSources.forEach(file -> {
                        sourceLogic.uploadSource(file, allSources.get(file).first, allSources.get(file).second);
                    });
                    CrowdinProjectCacheProvider.outdateBranch(branchName);
                } catch (ProcessCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    NotificationUtil.showErrorMessage(project, e.getMessage());
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
