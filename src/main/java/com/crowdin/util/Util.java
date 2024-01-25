package com.crowdin.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

import java.util.List;
import java.util.stream.Collectors;

public class Util {

    private static final String PLUGIN_NAME = "crowdin-android-studio-plugin";
    private static final String PLUGIN_ID = "com.crowdin.crowdin-idea";
    private static final PluginId PLUGIN = PluginId.getId(PLUGIN_ID);

    public static String getPluginVersion() {
//        since 193.5233.102
//        return PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)).getVersion();
        for (IdeaPluginDescriptor plugin : PluginManagerCore.getPlugins()) {
            if (PLUGIN == plugin.getPluginId()) {
                return plugin.getVersion();
            }
        }
        return "";
    }

    public static String getUserAgent() {
        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        return String.format("%s/%s %s/%s %s/%s",
                PLUGIN_NAME, getPluginVersion(),
                appInfo.getVersionName(), appInfo.getApiVersion(),
                System.getProperty("os.name"), System.getProperty("os.version"));
    }

    public static String prepareListMessageText(String mainText, List<String> items) {
        String itemsInOne = "<ul>" + items.stream().map(s -> "<li>" + s + "</li>\n").collect(Collectors.joining()) + "</ul>";
        return "<body><p>" + mainText + "</p>" + itemsInOne + "</body>";
    }

    public static boolean isFileFormatNotAllowed(Exception e) {
        return e.getMessage().contains("files are not allowed to upload in string-based projects") ||
                e.getMessage().contains("files are not allowed to upload in strings-based projects");
    }
}
