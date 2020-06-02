package com.crowdin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import static com.crowdin.Constants.PROPERTIES_FILE;

public class PropertyUtil {

    public static String getPropertyValue(String key, Project project) {
        if (key == null) {
            return "";
        }
        Properties properties = getProperties(project);
        if (properties != null && properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return "";
        }
    }

    public static Properties getProperties(Project project) {
        Properties properties = new Properties();
        VirtualFile propertiesFile = getCrowdinPropertyFile(project);
        if (propertiesFile == null) {
            return null;
        }
        try (InputStream in = new FileInputStream(propertiesFile.getCanonicalPath())) {
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static VirtualFile getCrowdinPropertyFile(Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null || !baseDir.isDirectory()) {
            System.out.println("Base dir not exist");
            return null;
        }
        VirtualFile child = baseDir.findChild(PROPERTIES_FILE);
        if (child != null && child.exists()) {
            return child;
        } else {
            return null;
        }
    }
}
