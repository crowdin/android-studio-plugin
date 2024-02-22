package com.crowdin.client.config;

import com.crowdin.util.FileUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static com.crowdin.Constants.CONFIG_FILE;
import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class CrowdinFileProvider {

    private static final Yaml YAML = new Yaml();

    public static Map<String, Object> load(Project project) {
        VirtualFile crowdinPropertyFile = getCrowdinConfigFile(project);
        if (crowdinPropertyFile == null) {
            return null;
        }
        try (InputStream in = crowdinPropertyFile.getInputStream()) {
            return YAML.load(in);
        } catch (ClassCastException e) {
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.config.has_errors") + "The Yaml file is not in the correct format");
        } catch (IOException e) {
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.config.has_errors") + e.getMessage());
        }
    }

    public static VirtualFile getCrowdinConfigFile(Project project) {
        VirtualFile baseDir = FileUtil.getProjectBaseDir(project);
        if (baseDir == null || !baseDir.isDirectory()) {
            System.out.println("Base dir not exist");
            return null;
        }
        VirtualFile child = baseDir.findChild(CONFIG_FILE);
        if (child != null && child.exists()) {
            return child;
        } else {
            return null;
        }
    }
}
