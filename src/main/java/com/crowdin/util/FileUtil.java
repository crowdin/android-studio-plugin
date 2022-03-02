package com.crowdin.util;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.NonNull;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public final class FileUtil {

    public static final String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();
    public static final String PATH_SEPARATOR_REGEX = "\\".equals(PATH_SEPARATOR) ? "\\\\" : PATH_SEPARATOR;

    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    public static VirtualFile getProjectBaseDir(Project project) {
        String baseDirString = project.getBasePath();
        return findVFileByPath(baseDirString);
    }

    public static VirtualFile findVFileByPath(String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    public static String findRelativePath(@NonNull VirtualFile baseDir, @NonNull VirtualFile file) {
        return StringUtils.removeStart(file.getCanonicalPath(), baseDir.getCanonicalPath())
            .replaceAll("^[\\\\/]+", "");
//        @AvailableSince("181.2784.17")
//        return VfsUtil.findRelativePath(baseDir, file, java.io.File.separatorChar);
    }

    public static VirtualFile getBaseDir(VirtualFile file, String relativePath) {
        VirtualFile dir = file;
        int depth = FileUtil.splitPath(relativePath.replaceAll("^[\\\\/]?\\*\\*[\\\\/]?", "")).length;
        for (int i = depth; i > 0; i--) {
            if (dir.getParent() == null) {
                break;
            }
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

    public static Predicate<String> filePathRegex(String filePathPattern, boolean preserveHierarchy) {
        if (preserveHierarchy) {
            return Pattern.compile("^" + PlaceholderUtil.formatSourcePatternForRegex(noSepAtStart(filePathPattern)) + "$").asPredicate();
        } else {
            List<String> sourcePatternSplits = Arrays.stream(splitPath(noSepAtStart(filePathPattern)))
                .map(PlaceholderUtil::formatSourcePatternForRegex)
                .collect(Collectors.toList());

            StringBuilder sourcePatternRegex = new StringBuilder();
            for (int i = 0; i < sourcePatternSplits.size()-1; i++) {
                sourcePatternRegex.insert(0, "(")
                    .append(sourcePatternSplits.get(i)).append(PATH_SEPARATOR_REGEX).append(")?");
            }
            sourcePatternRegex.insert(0, FileUtil.PATH_SEPARATOR_REGEX).insert(0, "^")
                .append(sourcePatternSplits.get(sourcePatternSplits.size()-1)).append("$");

            return Pattern.compile(sourcePatternRegex.toString()).asPredicate();
        }
    }

    public static void downloadFile(Object requestor, VirtualFile file, URL url) {
        try (InputStream data = url.openStream()) {
            FileUtil.downloadFile(requestor, file, data);
        } catch (IOException e) {
            throw new RuntimeException(String.format(MESSAGES_BUNDLE.getString("errors.download_file"), file.getPath(), e.getMessage()), e);
        }
    }

    public static void downloadFile(Object requestor, VirtualFile file, InputStream data) throws IOException {
        File tempFile = downloadTempFile(data);

        WriteAction.runAndWait(() -> {
            try (InputStream tempFileInput = new FileInputStream(tempFile); OutputStream fileOutput = file.getOutputStream(requestor)) {
                FileUtilRt.copy(tempFileInput, fileOutput);
            }
        });
    }

    public static File downloadTempFile(InputStream data) throws IOException {
        File tempFile = FileUtilRt.createTempFile(RandomStringUtils.randomAlphanumeric(9), ".crowdin.tmp", true);
        try (OutputStream tempFileOutput = new FileOutputStream(tempFile)) {
            FileUtilRt.copy(data, tempFileOutput);
        }
        return tempFile;
    }

    public static VirtualFile createIfNeededFilePath(Object requestor, VirtualFile root, String filePath) throws IOException {
        String[] splitFilePath = splitPath(noSepAtStart(filePath));
        return WriteAction.computeAndWait(() -> {
            VirtualFile dir = root;
            for (int i = 0; i < splitFilePath.length - 1; i++) {
                VirtualFile child = dir.findChild(splitFilePath[i]);
                dir = (child != null) ? child : dir.createChildDirectory(requestor, splitFilePath[i]);
            }
            VirtualFile file = dir.findChild(splitFilePath[splitFilePath.length-1]);
            return (file != null) ? file : dir.createChildData(requestor, splitFilePath[splitFilePath.length-1]);
        });
    }

    public static String normalizePath(String path) {
        return path.replaceAll("[\\\\/]+", SystemUtils.IS_OS_WINDOWS ? "\\\\" : "/");
    }

    public static String unixPath(String path) {
        return path.replaceAll("[\\\\/]+", "/");
    }

    public static String[] splitPath(String path) {
        return path.split("[\\\\/]+");
    }

    public static String joinPaths(String... paths) {
        return FileUtil.normalizePath(String.join(File.separator, paths));
    }

    public static String noSepAtStart(String path) {
        return path.replaceAll("^[\\\\/]+", "");
    }

    public static String sepAtStart(String path) {
        return File.separator + noSepAtStart(path);
    }

    public static String noSepAtEnd(String path) {
        return path.replaceAll("[\\\\/]+$", "");
    }

    public static String sepAtEnd(String path) {
        return noSepAtEnd(path) + PATH_SEPARATOR;
    }
}
