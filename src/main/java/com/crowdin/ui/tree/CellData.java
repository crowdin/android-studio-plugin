package com.crowdin.ui.tree;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class CellData {

    private static final Icon LOGO = IconLoader.getIcon("/icons/icon.svg", CellData.class);
    private static final Icon FOLDER = IconLoader.getIcon("/icons/folder.svg", CellData.class);
    private static final Icon FILE = IconLoader.getIcon("/icons/file.svg", CellData.class);

    private final String text;
    private final Icon icon;
    private final boolean isFile;

    public static CellData root(String text) {
        return new CellData(text, LOGO, false);
    }

    public static CellData folder(String text) {
        return new CellData(text, FOLDER, false);
    }

    public static CellData file(String text) {
        return new CellData(text, FILE, true);
    }

    private CellData(String text, Icon icon, boolean isFile) {
        this.text = text;
        this.icon = icon;
        this.isFile = isFile;
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


}
