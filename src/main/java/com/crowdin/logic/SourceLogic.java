package com.crowdin.logic;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.sourcefiles.model.*;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.Map;

import static com.crowdin.Constants.MESSAGES_BUNDLE;
import static com.crowdin.util.FileUtil.joinPaths;
import static com.crowdin.util.FileUtil.normalizePath;
import static com.crowdin.util.FileUtil.sepAtStart;
import static com.crowdin.util.FileUtil.unixPath;

public class SourceLogic {

    private final Project project;
    private final Crowdin crowdin;
    private final Map<String, FileInfo> filePaths;
    private final Map<String, Directory> dirPaths;
    private final Long branchId;
    private final CrowdinProperties properties;

    public SourceLogic(
        Project project,
        Crowdin crowdin, CrowdinProperties properties,
        Map<String, FileInfo> filePaths, Map<String, Directory> dirPaths, Long branchId
    ) {
        this.project = project;
        this.crowdin = crowdin;
        this.filePaths = filePaths;
        this.dirPaths = dirPaths;
        this.branchId = branchId;
        this.properties = properties;
    }

    public void uploadSource(VirtualFile source, String sourcePattern, String translationPattern) {
        try {
            VirtualFile pathToPattern = FileUtil.getBaseDir(source, sourcePattern);

            GeneralFileExportOptions exportOptions = new GeneralFileExportOptions();

            String path;
            String parentPath;
            if (properties.isPreserveHierarchy()) {
                String relativePathToPattern = FileUtil.findRelativePath(FileUtil.getProjectBaseDir(project), pathToPattern);
                String patternPathToFile = FileUtil.findRelativePath(pathToPattern, source.getParent());

                path = sepAtStart(normalizePath(joinPaths(relativePathToPattern, patternPathToFile, source.getName())));

                parentPath = sepAtStart(normalizePath(joinPaths(relativePathToPattern, patternPathToFile)));
                exportOptions.setExportPattern(sepAtStart(unixPath(joinPaths(relativePathToPattern, translationPattern))));
            } else {
                path = sepAtStart(source.getName());
                parentPath = "";
                exportOptions.setExportPattern(sepAtStart(translationPattern));
            }

            String outputName = FileUtil.noSepAtStart(path);
            if (filePaths.containsKey(path)) {
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.update"), outputName, sourcePattern));
                Long sourceId = filePaths.get(path).getId();
                Long storageId;
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.add_to_storage"), outputName));
                try (InputStream sourceStream = source.getInputStream()) {
                    storageId = crowdin.addStorage(source.getName(), sourceStream);
                }

                UpdateFileRequest updateFileRequest = RequestBuilder.updateFile(storageId, exportOptions);
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.update_request"), updateFileRequest));
                crowdin.updateSource(sourceId, updateFileRequest);
                NotificationUtil.showInformationMessage(project, "File '" + outputName + "' is updated");
            } else {
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.upload"), outputName, sourcePattern));
                String type = source.getFileType().getName().toLowerCase();
                Long directoryId = this.buildPath(crowdin, parentPath, dirPaths, branchId);
                Long storageId;
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.add_to_storage"), outputName));
                try (InputStream sourceStream = source.getInputStream()) {
                    storageId = crowdin.addStorage(source.getName(), sourceStream);
                }

                AddFileRequest addFileRequest = RequestBuilder.addFile(
                    storageId, source.getName(), (directoryId == null ? branchId : null), directoryId, type, exportOptions);
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.upload_request"), addFileRequest));
                crowdin.addSource(addFileRequest);
                NotificationUtil.showInformationMessage(project, "File '" + outputName + "' is uploaded");
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Couldn't upload source file: %s", e.getMessage()), e);
        }
    }

    private Long buildPath(Crowdin crowdin, String path, Map<String, Directory> dirs, Long branchId) {
        String[] dirNames = FileUtil.splitPath(path);
        StringBuilder builtPath = new StringBuilder();
        Directory parent = null;
        for (String dirName : dirNames) {
            if (StringUtils.isEmpty(dirName)) {
                continue;
            }
            builtPath.append(java.io.File.separator).append(dirName);
            if (dirs.containsKey(builtPath.toString())) {
                parent = dirs.get(builtPath.toString());
            } else {
                AddDirectoryRequest addDirectoryRequest = new AddDirectoryRequest();
                if (parent != null) {
                    addDirectoryRequest.setDirectoryId(parent.getId());
                } else {
                    addDirectoryRequest.setBranchId(branchId);
                }
                addDirectoryRequest.setName(dirName);
                parent = crowdin.addDirectory(addDirectoryRequest);
                dirs.put(builtPath.toString(), parent);
            }
        }
        return (parent != null) ? parent.getId() : null;
    }
}
