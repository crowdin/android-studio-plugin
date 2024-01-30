package com.crowdin.ui.tree;

import com.crowdin.client.bundles.model.Bundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class CellData {

    private static final Icon LOGO = IconLoader.getIcon("/icons/icon.svg", CellData.class);
    private static final Icon FOLDER = IconLoader.getIcon("/icons/folder.svg", CellData.class);
    private static final Icon FILE = IconLoader.getIcon("/icons/file.svg", CellData.class);

    private final String text;
    private final Icon icon;
    private final String file;
    private final Bundle bundle;

    private final boolean isRoot;

    public static CellData root(String text) {
        return new CellData(true, text, LOGO, null, null);
    }

    public static CellData folder(String text) {
        return new CellData(false, text, FOLDER, null, null);
    }

    public static CellData file(String text, String file) {
        return new CellData(false, text, FILE, file, null);
    }

    public static CellData bundle(Bundle bundle) {
        return new CellData(false, bundle.getName(), AllIcons.FileTypes.Archive, null, bundle);
    }

    private CellData(boolean isRoot, String text, Icon icon, String file, Bundle bundle) {
        this.isRoot = isRoot;
        this.text = text;
        this.icon = icon;
        this.file = file;
        this.bundle = bundle;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String getText() {
        return text;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isFile() {
        return file != null;
    }

    public String getFile() {
        return file;
    }

    public boolean isBundle() {
        return bundle != null;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public String toString() {
        return "CellData{" +
                "text='" + text + '\'' +
                ", isFile=" + this.isFile() +
                ", isBundle=" + this.isBundle() +
                '}';
    }
}
