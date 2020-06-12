package com.crowdin.util;

import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.File;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrowdinFileUtil {

    private CrowdinFileUtil() {}

    public static Map<String, File> buildFilePaths(@NonNull List<File> files, @NonNull Map<Long, Directory> dirs) {
        Map<String, File> filePaths = new HashMap<>();
        for (File file : files) {
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
}
