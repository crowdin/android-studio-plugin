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
    private final boolean isFile;
    private final Bundle bundle;

    public static CellData root(String text) {
        return new CellData(text, LOGO, false, null);
    }

    public static CellData folder(String text) {
        return new CellData(text, FOLDER, false, null);
    }

    public static CellData file(String text) {
        return new CellData(text, FILE, true, null);
    }

    public static CellData bundle(Bundle bundle) {
        return new CellData(bundle.getName(), AllIcons.FileTypes.Archive, false, bundle);
    }

    private CellData(String text, Icon icon, boolean isFile, Bundle bundle) {
        this.text = text;
        this.icon = icon;
        this.isFile = isFile;
        this.bundle = bundle;
    }

    public String getText() {
        return text;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isFile() {
        return isFile;
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
                ", isFile=" + isFile +
                '}';
    }
}
