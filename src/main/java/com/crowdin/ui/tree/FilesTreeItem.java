package com.crowdin.ui.tree;

import javax.swing.*;

public class FilesTreeItem {
    private JPanel content;
    private JLabel label;

    public FilesTreeItem(String text, Icon icon) {
        label.setText(text);
        label.setIcon(icon);
    }

    public void setIcon(Icon icon) {
        label.setIcon(icon);
    }

    public JPanel getContent() {
        return content;
    }
}
