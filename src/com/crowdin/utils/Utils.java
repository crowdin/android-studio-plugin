package com.crowdin.utils;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitLocalBranch;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by ihor on 1/24/17.
 */
public class Utils {

    public static final String PROPERTIES_FILE = "crowdin.properties";

    public static final String FILE_NAME = "strings.xml";

    public static final String PARENT_FOLDER_NAME = "values";

    public static final NotificationGroup GROUP_DISPLAY_ID_INFO =
            new NotificationGroup("Crowdin",
                    NotificationDisplayType.BALLOON, true);

    public static String getPropertyValue(String key) {
        if (key == null) {
            return "";
        }
        VirtualFile baseDir = ProjectManager.getInstance().getOpenProjects()[0].getBaseDir();
        if (baseDir == null || !baseDir.isDirectory()) {
            System.out.println("Base dir not exist");
            return "";
        }
        Properties properties = new Properties();
        String value = null;
        try {
            VirtualFile crowdinProperties = baseDir.findChild(PROPERTIES_FILE);
            InputStream in = new FileInputStream(crowdinProperties.getCanonicalPath());
            properties.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (properties != null && properties.get(key) != null) {
            value = properties.get(key).toString();
        }
        return value;
    }

    public static void showInformationMessage(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.INFORMATION);
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Notifications.Bus.notify(notification, projects[0]);
        });
    }

    public static VirtualFile getSourceFile(VirtualFile baseDir) {
        VirtualFile[] children = baseDir.getChildren();
        for (VirtualFile v : children) {
            if (v.isDirectory()) {
                VirtualFile vf = Utils.getSourceFile(v);
                if (vf != null) {
                    return vf;
                }
            } else {
                if (FILE_NAME.equals(v.getName()) && PARENT_FOLDER_NAME.equals(v.getParent().getName())) {
                    return v;
                }
            }
        }
        return null;
    }

    public static void extractTranslations(File archive) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(archive);
        } catch (ZipException e) {
            e.printStackTrace();
        }
        try {
            zipFile.extractAll(archive.getParent());
            Utils.showInformationMessage("Translations successfully downloaded");
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentBranch(@NotNull final Project project) {
        GitRepository repository = null;
        GitLocalBranch localBranch = null;
        String branchName = "";
        try {
            repository = GitBranchUtil.getCurrentRepository(project);
            localBranch = repository.getCurrentBranch();
            branchName = localBranch.getName();
        } catch (NullPointerException e) {
            e.getMessage();
        }
        return branchName;
    }
}
