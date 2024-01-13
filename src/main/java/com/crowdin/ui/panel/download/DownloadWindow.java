package com.crowdin.ui.panel.download;

import com.crowdin.ui.panel.ContentTab;

import javax.swing.*;

public class DownloadWindow implements ContentTab {
    private JTextField downloadDataTextField;
    private JPanel panel1;

    @Override
    public JPanel getContent() {
        return panel1;
    }

    public JTextField getDownloadDataTextField() {
        return downloadDataTextField;
    }
}
