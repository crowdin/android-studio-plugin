package com.crowdin;

import java.util.ResourceBundle;

public final class Constants {

    public static final String CONFIG_FILE = "crowdin.yml";

    public static final ResourceBundle MESSAGES_BUNDLE = ResourceBundle.getBundle("messages/messages");

    //UI Components Ids

    public static final String PROGRESS_TOOLBAR_ID = "Crowdin.TranslationProgressToolbar";
    public static final String TOOLWINDOW_ID = "Crowdin";
    public static final String UPLOAD_TOOLBAR_ID = "Crowdin.UploadToolbar";
    public static final String DOWNLOAD_TOOLBAR_ID = "Crowdin.DownloadToolbar";

    //Actions Ids

    public static final String PROGRESS_REFRESH_ACTION = "Crowdin.RefreshTranslationProgressAction";
    public static final String UPLOAD_REFRESH_ACTION = "Crowdin.RefreshUploadAction";
    public static final String DOWNLOAD_REFRESH_ACTION = "Crowdin.RefreshDownloadAction";
    public static final String DOWNLOAD_TRANSLATIONS_ACTION = "Crowdin.DownloadTranslations";
    public static final String DOWNLOAD_SOURCES_ACTION = "Crowdin.DownloadSources";
    public static final String BUNDLE_SETTINGS_ACTION = "Crowdin.BundleSettings";
    public static final String PROGRESS_GROUP_FILES_BY_FILE_ACTION = "Crowdin.GroupByFiles";
}
