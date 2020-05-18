package com.crowdin.util;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class FileUtil {

    private static final String FILE_NAME = "strings.xml";

    public static final String PARENT_FOLDER_NAME = "values";

    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    public static List<String> getSourcesList(String sources) {
        List<String> result = new LinkedList<>();
        if (sources == null || sources.isEmpty()) {
            result.add(FILE_NAME);
            return result;
        }
        String[] sourceNodes = sources.trim().split(",");
        for (String src : sourceNodes) {
            if (src != null && !src.isEmpty()) {
                result.add(src.trim());
            }
        }
        return result;
    }

    public static VirtualFile getSourceFile(VirtualFile baseDir, String fileName) {
        fileName = (fileName != null && !fileName.isEmpty()) ? fileName : FILE_NAME;
        VirtualFile[] children = baseDir.getChildren();
        for (VirtualFile v : children) {
            if (v.isDirectory()) {
                VirtualFile vf = getSourceFile(v, fileName);
                if (vf != null) {
                    return vf;
                }
            } else {
                if (fileName.equals(v.getName()) && PARENT_FOLDER_NAME.equals(v.getParent().getName())) {
                    return v;
                }
            }
        }
        return null;
    }

    public static List<File> walkDir(Path dir) {
        try {
            return java.nio.file.Files.walk(dir)
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
