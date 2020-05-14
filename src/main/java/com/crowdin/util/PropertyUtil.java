package com.crowdin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {

    public static final String PROPERTIES_FILE = "crowdin.properties";

    public static final String PROPERTY_SOURCES = "sources";
    public static final String PROPERTY_DISABLE_BRANCHES = "disable-branches";

    public static String getPropertyValue(String key, Project project) {
        if (key == null) {
            return "";
        }
        Properties properties = new Properties();
        try {
            VirtualFile crowdinProperties = getCrowdinPropertyFile(project);
            if (crowdinProperties == null) {
                return "";
            }
            InputStream in = new FileInputStream(crowdinProperties.getCanonicalPath());
            properties.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return "";
        }
    }

    public static VirtualFile getCrowdinPropertyFile(Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null || !baseDir.isDirectory()) {
            System.out.println("Base dir not exist");
            return null;
        }
        return baseDir.findChild(PROPERTIES_FILE);
    }
}
