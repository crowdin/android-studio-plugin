package com.crowdin.event;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.CrowdinPropertiesLoader;
import com.crowdin.client.FileBean;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.logic.BranchLogic;
import com.crowdin.logic.SourceLogic;
import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.service.ProjectService;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
//        do nothing
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
                    Crowdin crowdin = new Crowdin(properties.getProjectId(), properties.getApiToken(), properties.getBaseUrl());

                    BranchLogic branchLogic = new BranchLogic(crowdin, project, properties);
                    String branchName = branchLogic.acquireBranchName();

                    CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache =
                            project.getService(CrowdinProjectCacheProvider.class).getInstance(crowdin, branchName, false);
                    indicator.checkCanceled();

                    Map<FileBean, List<VirtualFile>> allSources = new HashMap<>();
                    for (FileBean fileBean : properties.getFiles()) {
                        allSources.put(fileBean, FileUtil.getSourceFilesRec(FileUtil.getProjectBaseDir(project), fileBean.getSource()));
                    }

                    Map<FileBean, List<VirtualFile>> changedSources = new HashMap<>();
                    for (VFileEvent event : events) {
                        VirtualFile eventFile = event.getFile();
                        if (eventFile != null) {
                            for (FileBean fileBean : properties.getFiles()) {
                                if (allSources.get(fileBean).contains(eventFile)) {
                                    changedSources.putIfAbsent(fileBean, new ArrayList<>());
                                    changedSources.get(fileBean).add(eventFile);
                                    break;
                                }
                            }
                        }
                    }

                    VirtualFile crowdinPropertyFile = PropertyUtil.getCrowdinPropertyFile(project);
                    boolean crowdinPropertiesFileUpdated = events.stream().anyMatch(e -> Objects.equals(e.getFile(), crowdinPropertyFile));
                    if (crowdinPropertiesFileUpdated) {
                        ProjectService projectService = project.getService(ProjectService.class);
                        if (projectService.getLoadedComponents().contains(ProjectService.InitializationItem.UI_PANELS)) {
                            CrowdinPanelWindowFactory.reloadPanels(project, false);
                        }
                    }

                    if (changedSources.isEmpty()) {
                        return;
                    }

                    Branch branch = branchLogic.getBranch(crowdinProjectCache, true);
                    indicator.checkCanceled();

                    String text = changedSources.values().stream()
                            .flatMap(Collection::stream)
                            .map(VirtualFile::getName)
                            .collect(Collectors.joining(","));
                    indicator.setText(String.format(MESSAGES_BUNDLE.getString("messages.uploading_file_s"), text, changedSources.size() == 1 ? "" : "s"));

                    SourceLogic.processSources(project, FileUtil.getProjectBaseDir(project), crowdin, crowdinProjectCache, branch, properties.isPreserveHierarchy(), changedSources);

                    project.getService(CrowdinProjectCacheProvider.class).outdateBranch(branchName);
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
