package com.crowdin.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

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
}
