package com.crowdin.ui.tree;

import com.crowdin.client.bundles.model.Bundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.util.Map;
import java.util.Optional;

public class CellData {

    private static final Icon LOGO = IconLoader.getIcon("/icons/icon.svg", CellData.class);

    private static final Map<String, Icon> FILES_TYPES_ICONS = Map.of(
            "xml", AllIcons.FileTypes.Xml,
            "json", AllIcons.FileTypes.Json,
            "html", AllIcons.FileTypes.Html,
            "xsd", AllIcons.FileTypes.XsdFile,
            "css", AllIcons.FileTypes.Css,
            "yml", AllIcons.FileTypes.Yaml,
            "yaml", AllIcons.FileTypes.Yaml
    );

    private final String text;
    private final Icon icon;
    private final String file;
    private final Bundle bundle;

    private final boolean isRoot;

    private final String link;

    public static CellData root(String text) {
        return new CellData(true, text, LOGO, null, null, null);
    }

    public static CellData folder(String text) {
        return new CellData(false, text, AllIcons.Nodes.Folder, null, null, null);
    }

    public static CellData file(String text, String file) {
        String extension = FilenameUtils.getExtension(file);
        Icon icon = Optional.ofNullable(extension)
                .filter(e -> !e.isEmpty())
                .map(String::toLowerCase)
                .filter(FILES_TYPES_ICONS::containsKey)
                .map(FILES_TYPES_ICONS::get)
                .orElse(AllIcons.FileTypes.Text);
        return new CellData(false, text, icon, file, null, null);
    }

    public static CellData bundle(Bundle bundle) {
        return new CellData(false, bundle.getName(), AllIcons.FileTypes.Archive, null, bundle, null);
    }

    public static CellData link(String text, String link) {
        return new CellData(false, text, AllIcons.Ide.Link, null, null, link);
    }

    private CellData(boolean isRoot, String text, Icon icon, String file, Bundle bundle, String link) {
        this.isRoot = isRoot;
        this.text = text;
        this.icon = icon;
        this.file = file;
        this.bundle = bundle;
        this.link = link;
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

    public boolean isLink() {
        return link != null;
    }

    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "CellData{" +
                "text='" + text + '\'' +
                ", icon=" + icon +
                ", file='" + file + '\'' +
                ", bundle=" + bundle +
                ", isRoot=" + isRoot +
                ", link='" + link + '\'' +
                '}';
    }
}
