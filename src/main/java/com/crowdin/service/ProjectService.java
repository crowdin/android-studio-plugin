package com.crowdin.service;

import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.ui.panel.upload.UploadWindow;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectService {

    private TranslationProgressWindow translationProgressWindow;
    private UploadWindow uploadWindow;
    private DownloadWindow downloadWindow;
    private final EnumSet<InitializationItem> initializationItems = EnumSet.noneOf(InitializationItem.class);

    public void setTranslationProgressWindow(TranslationProgressWindow translationProgressWindow) {
        this.translationProgressWindow = translationProgressWindow;
    }

    public TranslationProgressWindow getTranslationProgressWindow() {
        return translationProgressWindow;
    }

    public UploadWindow getUploadWindow() {
        return uploadWindow;
    }

    public void setUploadWindow(UploadWindow uploadWindow) {
        this.uploadWindow = uploadWindow;
    }

    public DownloadWindow getDownloadWindow() {
        return downloadWindow;
    }

    public void setDownloadWindow(DownloadWindow downloadWindow) {
        this.downloadWindow = downloadWindow;
    }

    public synchronized EnumSet<InitializationItem> addAndGetLoadedComponents(InitializationItem item) {
        initializationItems.add(item);
        return initializationItems;
    }

    public static enum InitializationItem {
        STARTUP_ACTIVITY, UI_PANELS
    }
}
