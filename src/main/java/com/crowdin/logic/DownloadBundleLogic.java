package com.crowdin.logic;

import com.crowdin.client.Crowdin;
import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.client.bundles.model.BundleExport;
import com.crowdin.util.FileUtil;
import com.crowdin.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.crowdin.Constants.MESSAGES_BUNDLE;

public class DownloadBundleLogic {

    private final Project project;
    private final Crowdin crowdin;
    private final VirtualFile root;
    private final Bundle bundle;

    public DownloadBundleLogic(Project project, Crowdin crowdin, VirtualFile root, Bundle bundle) {
        this.project = project;
        this.crowdin = crowdin;
        this.root = root;
        this.bundle = bundle;
    }

    public void process() {
        File archive = null;
        try {
            archive = downloadBundleArchive();

            NotificationUtil.logDebugMessage(project, String.format(MESSAGES_BUNDLE.getString("messages.debug.download.extract_files"), archive));

            FileUtil.extractArchive(archive, this.root.getPath());
        } finally {
            NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.download.clearing"));
            FileUtil.clear(root, archive, null);
        }
    }

    public File downloadBundleArchive() {
        NotificationUtil.logDebugMessage(project, MESSAGES_BUNDLE.getString("messages.debug.download.download_archive"));

        BundleExport bundleExport = crowdin.startBuildingBundle(this.bundle.getId());
        String buildId = bundleExport.getIdentifier();

        while (!bundleExport.getStatus().equalsIgnoreCase("finished")) {
            bundleExport = crowdin.checkBundleBuildingStatus(bundle.getId(), buildId);
        }

        URL url = crowdin.downloadBundle(bundle.getId(), buildId);

        try {
            return FileUtil.downloadTempFile(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't download file", e);
        }
    }
}
