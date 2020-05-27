package com.crowdin.util;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class FileUtil {

    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    public static VirtualFile getBaseDir(VirtualFile file, String relativePath) {
        VirtualFile dir = file;
        for (int i = relativePath.split("[\\\\/]+").length; i > 0; i--) {
            dir = dir.getParent();
        }
        return dir;
    }

    public static List<VirtualFile> getSourceFilesRec(VirtualFile root, String source) {
        int sepIndex = source.indexOf("/");
        List<VirtualFile> files = new ArrayList<>();
        boolean isDir = sepIndex != -1;
        String searchable = (isDir) ? source.substring(0, sepIndex) : source;
        if ("**".equals(searchable) && isDir) {
            for (VirtualFile child : root.getChildren()) {
                if (child.isDirectory()) {
                    files.addAll(getSourceFilesRec(child, source));
                }
            }
            files.addAll(getSourceFilesRec(root, source.substring(sepIndex + 1)));
        } else if (searchable.contains("*")) {
            String searchableRegex = searchable.replace("*", ".*");
            for (VirtualFile child : root.getChildren()) {
                if (!child.isDirectory() && !isDir && child.getName().matches(searchableRegex)) {
                    files.add(child);
                } else if (child.isDirectory() && isDir && child.getName().matches(searchableRegex)) {
                    files.addAll(getSourceFilesRec(child, source.substring(sepIndex+1)));
                }
            }
        } else {
            VirtualFile foundChild = root.findChild(searchable);
            if (foundChild != null) {
                if (foundChild.isDirectory() && isDir) {
                    files.addAll(getSourceFilesRec(foundChild, source.substring(sepIndex+1)));
                } else if (!foundChild.isDirectory() && !isDir) {
                    files.add(foundChild);
                }
            } else {
                for (VirtualFile child : root.getChildren()) {
                    files.addAll(getSourceFilesRec(child, source));
                }
            }
        }
        return files;
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
