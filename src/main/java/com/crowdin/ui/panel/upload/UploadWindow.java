package com.crowdin.ui.panel.upload;

import com.crowdin.ui.panel.ContentTab;

import javax.swing.*;

public class UploadWindow implements ContentTab {
    private JTextField uploadDataTextField;
    private JPanel panel1;

    @Override
    public JPanel getContent() {
        return panel1;
    }
}
