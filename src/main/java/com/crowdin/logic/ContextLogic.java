package com.crowdin.logic;

import com.crowdin.service.CrowdinProjectCacheProvider;
import com.crowdin.client.config.CrowdinConfig;
import com.crowdin.client.config.FileBean;
import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.util.FileUtil;
import com.crowdin.util.PlaceholderUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class ContextLogic {

    public static Optional<Map.Entry<VirtualFile, Language>> findSourceFileFromTranslationFile(
            VirtualFile file, CrowdinConfig properties, VirtualFile root, CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache
    ) {
        Path filePath = Paths.get(file.getPath());
        for (FileBean fileBean : properties.getFiles()) {
            for (VirtualFile source : FileUtil.getSourceFilesRec(root, fileBean.getSource())) {
                VirtualFile baseDir = FileUtil.getBaseDir(source, fileBean.getSource());
                String sourcePath = source.getName();
                String basePattern = PlaceholderUtil.replaceFilePlaceholders(fileBean.getTranslation(), sourcePath);
                for (Language lang : crowdinProjectCache.getProjectLanguages()) {
                    String builtPattern = PlaceholderUtil.replaceLanguagePlaceholders(basePattern, lang, crowdinProjectCache.getLanguageMapping());
                    Path translationFile = Paths.get(baseDir.getPath(), builtPattern);
                    if (filePath.equals(translationFile)) {
                        return Optional.of(new AbstractMap.SimpleImmutableEntry<>(source, lang));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Long findSourceIdFromSourceFile(
            CrowdinConfig properties, Map<String, FileInfo> filePaths, VirtualFile file, VirtualFile root
    ) {
        String fileRelativePath = FileUtil.sepAtStart(FileUtil.findRelativePath(root, file));
        if (properties.isPreserveHierarchy()) {
            if (filePaths.containsKey(fileRelativePath)) {
                FileInfo foundSource = filePaths.get(fileRelativePath);
                return foundSource.getId();
            } else {
                throw new RuntimeException(
                        String.format(MESSAGES_BUNDLE.getString("errors.file_no_server_representative"), fileRelativePath)
                );
            }
        } else {
            List<String> foundCrowdinSources = filePaths.keySet().stream()
                    .filter(crowdinFilePath -> file.getPath().endsWith(crowdinFilePath))
                    .toList();
            if (foundCrowdinSources.isEmpty()) {
                throw new RuntimeException(
                        String.format(MESSAGES_BUNDLE.getString("errors.file_no_server_representative"), fileRelativePath)
                );
            } else if (foundCrowdinSources.size() > 1) {
                throw new RuntimeException(
                        String.format(MESSAGES_BUNDLE.getString("errors.file_not_one_server_representative"), fileRelativePath)
                );
            } else {
                return filePaths.get(foundCrowdinSources.get(0)).getId();
            }
        }
    }
}
