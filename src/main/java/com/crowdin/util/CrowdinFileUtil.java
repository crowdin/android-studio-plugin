package com.crowdin.util;

import com.crowdin.client.languages.model.Language;
import com.crowdin.client.sourcefiles.model.*;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CrowdinFileUtil {

    private CrowdinFileUtil() {}

    public static <F extends FileInfo>  Map<String, F> buildFilePaths(@NonNull List<F> files, @NonNull Map<Long, Directory> dirs) {
        Map<String, F> filePaths = new HashMap<>();
        for (F file : files) {
            StringBuilder sb = new StringBuilder(file.getName());
            Long parentDirId = file.getDirectoryId();
            while (parentDirId != null) {
                Directory parentDir = dirs.get(parentDirId);
                sb.insert(0, parentDir.getName() + java.io.File.separator);
                parentDirId = parentDir.getDirectoryId();
            }
            sb.insert(0, java.io.File.separator);
            filePaths.put(sb.toString(), file);
        }
        return filePaths;
    }

    public static Map<String, Directory> buildDirPaths(@NonNull Map<Long, Directory> dirs) {
        Map<String, Directory> dirPaths = new HashMap<>();
        for (Directory dir : dirs.values()) {
            StringBuilder sb = new StringBuilder(dir.getName());
            Long parentDirId = dir.getDirectoryId();
            while (parentDirId != null) {
                Directory parentDir = dirs.get(parentDirId);
                sb.insert(0, parentDir.getName() + java.io.File.separator);
                parentDirId = parentDir.getDirectoryId();
            }
            sb.insert(0, java.io.File.separator);
            dirPaths.put(sb.toString(), dir);
        }
        return dirPaths;
    }

    public static Map<Long, String> revDirPaths(@NonNull Map<String, Directory> dirs) {
        return dirs.keySet().stream()
            .collect(Collectors.toMap(path -> dirs.get(path).getId(), Function.identity()));
    }

    public static Map<String, String> buildAllProjectTranslationsWithSources(@NonNull List<File> sources, @NonNull Map<Long, String> dirPaths, @NonNull List<Language> projLanguages, LanguageMapping languageMapping) {
        Map<String, String> result = new HashMap<>();
        for (File source : sources) {
            String sourcePath = ((source.getDirectoryId() != null) ? dirPaths.get(source.getDirectoryId()) + java.io.File.separator : java.io.File.separator) + source.getName();
            if (source.getExportOptions() != null) {
                for (Language lang : projLanguages) {
                    String langBasedPattern = PlaceholderUtil.replaceLanguagePlaceholders(getExportPattern(source.getExportOptions()), lang, languageMapping);
                    String translationPath = PlaceholderUtil.replaceFilePlaceholders(langBasedPattern, sourcePath);
                    result.put(translationPath, sourcePath);
                }
            } else {
                result.put(sourcePath, sourcePath);
            }
        }
        return result;
    }


    private static String getExportPattern(ExportOptions exportOptions) {
        if (exportOptions instanceof GeneralFileExportOptions) {
            return ((GeneralFileExportOptions) exportOptions).getExportPattern();
        } else if (exportOptions instanceof PropertyFileExportOptions) {
            return ((PropertyFileExportOptions) exportOptions).getExportPattern();
        } else {
            throw new RuntimeException(String.format("Unexpected export pattern: %s", exportOptions.toString()));
        }
    }

    public static boolean isValidBranchName(String branchName) {
        return StringUtils.containsNone(branchName, "/\\:*?\"<>");
    }
}
