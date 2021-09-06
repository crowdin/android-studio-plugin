package com.crowdin.logic;

import com.crowdin.client.Crowdin;
import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.client.FileBean;
import com.crowdin.client.RequestBuilder;
import com.crowdin.client.core.model.PatchRequest;
import com.crowdin.client.labels.model.Label;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.*;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;
import static com.crowdin.util.FileUtil.joinPaths;
import static com.crowdin.util.FileUtil.normalizePath;
import static com.crowdin.util.FileUtil.sepAtStart;
import static com.crowdin.util.FileUtil.unixPath;

public class SourceLogic {

    private final VirtualFile root;
    private final Project project;
    private final Crowdin crowdin;
    private final Map<String, FileInfo> filePaths;
    private final Map<String, Directory> dirPaths;
    private final Map<String, Long> labels;
    private final Long branchId;

    public static void processSources(
        Project project, VirtualFile root,
        Crowdin crowdin, CrowdinProjectCacheProvider.CrowdinProjectCache projectCache,
        Branch branch, boolean preserveHierarchy, Map<FileBean, List<VirtualFile>> sourcesToUpload
    ) {
        Map<String, FileInfo> filePaths = projectCache.getFileInfos(branch);
        Map<String, Directory> dirPaths = projectCache.getDirs().getOrDefault(branch, new HashMap<>());
        Map<String, Long> labels = SourceLogic.prepareLabels(crowdin, new ArrayList<>(sourcesToUpload.keySet()));
        Long branchId = (branch != null) ? branch.getId() : null;

        SourceLogic sourceLogic = new SourceLogic(root, project, crowdin, filePaths, dirPaths, labels, branchId);
        for (FileBean fileBean : sourcesToUpload.keySet()) {
            if (fileBean.getExcludedTargetLanguages() != null && !fileBean.getExcludedTargetLanguages().isEmpty()) {
                SourceLogic.checkExcludedTargetLanguages(fileBean.getExcludedTargetLanguages(), projectCache.getSupportedLanguages(), projectCache.getProjectLanguages());
            }
        }
        for (FileBean fileBean : sourcesToUpload.keySet()) {
            for (VirtualFile source : sourcesToUpload.get(fileBean)) {
                try {
                    sourceLogic.uploadSource(source, fileBean, preserveHierarchy);
                } catch (Exception e) {
                    NotificationUtil.logErrorMessage(project, e);
                    NotificationUtil.showErrorMessage(project, e.getMessage());
                }
            }
        }
    }

    public SourceLogic(
        VirtualFile root, Project project,
        Crowdin crowdin,
        Map<String, FileInfo> filePaths, Map<String, Directory> dirPaths, Map<String, Long> labelMap, Long branchId
    ) {
        this.root = root;
        this.project = project;
        this.crowdin = crowdin;
        this.filePaths = filePaths;
        this.dirPaths = dirPaths;
        this.labels = labelMap;
        this.branchId = branchId;
    }

    public void uploadSource(VirtualFile source, FileBean fileBean, boolean preserveHierarchy) {
        try {
            VirtualFile pathToPattern = FileUtil.getBaseDir(source, fileBean.getSource());

            GeneralFileExportOptions exportOptions = new GeneralFileExportOptions();

            String path;
            String parentPath;
            if (preserveHierarchy) {
                String relativePathToPattern = FileUtil.findRelativePath(root, pathToPattern);
                String patternPathToFile = FileUtil.findRelativePath(pathToPattern, source.getParent());

                path = sepAtStart(normalizePath(joinPaths(relativePathToPattern, patternPathToFile, source.getName())));

                parentPath = sepAtStart(normalizePath(joinPaths(relativePathToPattern, patternPathToFile)));
                exportOptions.setExportPattern(sepAtStart(unixPath(joinPaths(relativePathToPattern, fileBean.getTranslation()))));
            } else {
                path = sepAtStart(source.getName());
                parentPath = "";
                exportOptions.setExportPattern(sepAtStart(fileBean.getTranslation()));
            }

            String outputName = FileUtil.noSepAtStart(path);
            FileInfo foundFile = filePaths.get(path);
            if (foundFile != null) {
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.update"), outputName, fileBean.getSource()));
                Long sourceId = filePaths.get(path).getId();
                Long storageId;
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.add_to_storage"), outputName));
                try (InputStream sourceStream = source.getInputStream()) {
                    storageId = crowdin.addStorage(source.getName(), sourceStream);
                }


                UpdateFileRequest updateFileRequest = RequestBuilder.updateFile(storageId, exportOptions);

                if (fileBean.getLabels() != null && !fileBean.getLabels().isEmpty()) {
                    List<Long> labelIds = fileBean.getLabels().stream().map(labels::get).collect(Collectors.toList());
                    updateFileRequest.setAttachLabelIds(labelIds);
                }

                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.update_request"), updateFileRequest));
                crowdin.updateSource(sourceId, updateFileRequest);
                if (fileBean.getExcludedTargetLanguages() != null && !fileBean.getExcludedTargetLanguages().isEmpty()) {
                    List<String> projectFileExcludedTargetLanguages = ((com.crowdin.client.sourcefiles.model.File) foundFile).getExcludedTargetLanguages();
                    if (!fileBean.getExcludedTargetLanguages().equals(projectFileExcludedTargetLanguages)) {
                        List<PatchRequest> editRequest = RequestBuilder.updateExcludedTargetLanguages(fileBean.getExcludedTargetLanguages());
                        crowdin.editSource(sourceId, editRequest);
                    }
                }
                NotificationUtil.showInformationMessage(project, "File '" + outputName + "' is updated");
            } else {
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.upload"), outputName, fileBean.getSource()));
                String type = source.getFileType().getName().toLowerCase();
                Long directoryId = this.buildPath(crowdin, parentPath, dirPaths, branchId);
                Long storageId;
                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.add_to_storage"), outputName));
                try (InputStream sourceStream = source.getInputStream()) {
                    storageId = crowdin.addStorage(source.getName(), sourceStream);
                }

                AddFileRequest addFileRequest = RequestBuilder.addFile(
                    storageId, source.getName(), (directoryId == null ? branchId : null), directoryId, type, exportOptions);

                if (fileBean.getLabels() != null && !fileBean.getLabels().isEmpty()) {
                    List<Long> labelIds = fileBean.getLabels().stream().map(labels::get).collect(Collectors.toList());
                    addFileRequest.setAttachLabelIds(labelIds);
                }
                if (fileBean.getExcludedTargetLanguages() != null && !fileBean.getExcludedTargetLanguages().isEmpty()) {
                    addFileRequest.setExcludedTargetLanguages(fileBean.getExcludedTargetLanguages());
                }

                NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.upload_sources.upload_request"), addFileRequest));
                crowdin.addSource(addFileRequest);
                NotificationUtil.showInformationMessage(project, "File '" + outputName + "' is uploaded");
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Couldn't upload source file: %s", e.getMessage()), e);
        }
    }

    private boolean isFileUploaded(String path) {
        return filePaths.containsKey(path);
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

    public static void checkExcludedTargetLanguages(List<String> excludedTargetLanguages, List<Language> supportedLanguages, List<Language> projectLanguages) {
        if (excludedTargetLanguages != null && !excludedTargetLanguages.isEmpty()) {
            List<String> supportedLanguageIds = supportedLanguages.stream()
                .map(Language::getId)
                .collect(Collectors.toList());;
            List<String> projectLanguageIds = projectLanguages.stream()
                .map(Language::getId)
                .collect(Collectors.toList());;
            String notSupportedLangs = excludedTargetLanguages.stream()
                .filter(lang -> !supportedLanguageIds.contains(lang))
                .map(lang -> "'" + lang + "'")
                .collect(Collectors.joining(", "));
            if (notSupportedLangs.length() > 0) {
                throw new RuntimeException(String.format("Crowdin doesn't support %s language code(s)", notSupportedLangs));
            }
            String notInProjectLangs = excludedTargetLanguages.stream()
                .filter(lang -> !projectLanguageIds.contains(lang))
                .map(lang -> "'" + lang + "'")
                .collect(Collectors.joining(", "));
            if (notInProjectLangs.length() > 0) {
                throw new RuntimeException(String.format("Project doesn't have %s language(s)", notInProjectLangs));
            }
        }
    }

    public static Map<String, Long> prepareLabels(Crowdin crowdin, List<FileBean> fileBeans) {
        Map<String, Long> labels = crowdin.listLabels().stream()
            .collect(Collectors.toMap(Label::getTitle, Label::getId));
        fileBeans.stream()
            .map(FileBean::getLabels)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .distinct()
            .forEach(labelTitle -> labels.computeIfAbsent(labelTitle, (title) -> crowdin.addLabel(RequestBuilder.addLabel(title)).getId()));
        return labels;
    }

}
