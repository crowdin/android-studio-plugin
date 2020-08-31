package com.crowdin.logic;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProperties;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.sourcefiles.model.*;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class SourceLogic {

    private final Project project;
    private final Crowdin crowdin;
    private final Map<String, File> filePaths;
    private final Map<String, Directory> dirPaths;
    private final Long branchId;
    private final CrowdinProperties properties;

    public SourceLogic(
        Project project,
        Crowdin crowdin, CrowdinProperties properties,
        Map<String, File> filePaths, Map<String, Directory> dirPaths, Long branchId
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

            String relativePathToPattern = (properties.isPreserveHierarchy())
                ? java.io.File.separator + FileUtil.findRelativePath(FileUtil.getProjectBaseDir(project), pathToPattern)
                : "";
            String patternPathToFile = (properties.isPreserveHierarchy())
                ? java.io.File.separator + FileUtil.findRelativePath(pathToPattern, source.getParent())
                : "";

            GeneralFileExportOptions exportOptions = new GeneralFileExportOptions();
            exportOptions.setExportPattern(FileUtil.unixPath(FileUtil.joinPaths(relativePathToPattern, translationPattern)));

            String outputName = FileUtil.noSepAtStart(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName()));
            if (filePaths.containsKey(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName()))) {
                Long sourceId = filePaths.get(FileUtil.joinPaths(relativePathToPattern, patternPathToFile, source.getName())).getId();
                Long storageId = crowdin.addStorage(source.getName(), source.getInputStream());

                UpdateFileRequest updateFileRequest = RequestBuilder.updateFile(storageId, exportOptions);
                crowdin.updateSource(sourceId, updateFileRequest);
                NotificationUtil.showInformationMessage(project, "File '" + outputName + "' is updated");
            } else {
                String type = source.getFileType().getName().toLowerCase();
                Long directoryId = this.buildPath(crowdin, FileUtil.joinPaths(relativePathToPattern, patternPathToFile), dirPaths, branchId);
                Long storageId = crowdin.addStorage(source.getName(), source.getInputStream());

                AddFileRequest addFileRequest = RequestBuilder.addFile(
                    storageId, source.getName(), (directoryId == null ? branchId : null), directoryId, type, exportOptions);
                crowdin.addSource(addFileRequest);
                NotificationUtil.showInformationMessage(project, "File '" + outputName + "' is uploaded");
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't upload source file", e);
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
