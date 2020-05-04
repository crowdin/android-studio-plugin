package com.crowdin.utils;

import com.crowdin.command.Crowdin;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Created by ihor on 1/24/17.
 */
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static final String PROPERTIES_FILE = "crowdin.properties";

    public static final String FILE_NAME = "strings.xml";

    public static final String PARENT_FOLDER_NAME = "values";

    public static final NotificationGroup GROUP_DISPLAY_ID_INFO =
            new NotificationGroup("Crowdin",
                    NotificationDisplayType.BALLOON, true);

    public static String getPropertyValue(String key, boolean isOptional) {
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
            if (crowdinProperties == null) {
                showInformationMessage("File '" + PROPERTIES_FILE + "' with Crowdin plugin configuration doesn't exist in project root directory");
                LOGGER.info("File '" + PROPERTIES_FILE + "' with Crowdin plugin configuration doesn't exist in project root directory");
                return "";
            }
            InputStream in = new FileInputStream(crowdinProperties.getCanonicalPath());
            properties.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (properties != null && properties.get(key) != null) {
            value = properties.get(key).toString();
        } else if (!isOptional) {
            showInformationMessage("Check does property '" + key + "' exist in your configuration file '" + PROPERTIES_FILE + "'");
            LOGGER.info("Check does property '" + key + "' exist in your configuration file '" + PROPERTIES_FILE + "'");
        }
        return value;
    }

    public static List<String> getSourcesList(String sources) {
        List<String> result = new LinkedList<>();
        if (sources == null || sources.isEmpty()) {
            result.add("strings.xml");
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

    public static void showInformationMessage(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.INFORMATION);
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Notifications.Bus.notify(notification, projects[0]);
        });
    }

    public static VirtualFile getSourceFile(VirtualFile baseDir, String fileName) {
        fileName = (fileName != null && !fileName.isEmpty()) ? fileName : FILE_NAME;
        VirtualFile[] children = baseDir.getChildren();
        for (VirtualFile v : children) {
            if (v.isDirectory()) {
                VirtualFile vf = Utils.getSourceFile(v, fileName);
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
            Utils.showInformationMessage("Downloading translations failed");
            e.printStackTrace();
        }
    }

    public static String getCurrentBranch(@NotNull final Project project) {
        String disableBranches = Utils.getPropertyValue(Crowdin.CROWDIN_DISABLE_BRANCHES, true);

        if(disableBranches != null && disableBranches.equals("true")) {
            return "";
        }

        GitRepository repository;
        GitLocalBranch localBranch;
        String branchName = "";
        try {
            repository = GitBranchUtil.getCurrentRepository(project);
            localBranch = repository.getCurrentBranch();
            branchName = localBranch.getName();
        } catch (Exception e) {
            e.getMessage();
        }
        if (branchName == null) {
            branchName = "";
        }
        return branchName;
    }
}