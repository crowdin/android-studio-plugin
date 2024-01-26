package com.crowdin.service;

import com.crowdin.ui.panel.download.DownloadWindow;
import com.crowdin.ui.panel.progress.TranslationProgressWindow;
import com.crowdin.ui.panel.upload.UploadWindow;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectService {

    private TranslationProgressWindow translationProgressWindow;
    private UploadWindow uploadWindow;
    private DownloadWindow downloadWindow;
    private final AtomicBoolean panelsLoaded = new AtomicBoolean(false);

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

    public AtomicBoolean getPanelsLoaded() {
        return panelsLoaded;
    }
}
