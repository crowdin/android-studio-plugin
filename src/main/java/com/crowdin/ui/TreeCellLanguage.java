package com.crowdin.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class TreeCellLanguage {
    private JPanel content;
    private JLabel translatedLabel;
    private JLabel approvedLabel;
    private JLabel mainLabel;
    private JLabel additionalLabel1;
    private JLabel additionalLabel2;
    private JLabel additionalLabel3;

    public TreeCellLanguage(String text) {
        content.setToolTipText("(translated, approved)");
        mainLabel.setText(text);
    }

    public void setIcon(Icon icon) {
        mainLabel.setIcon(icon);
    }

    public void showProgress(boolean show) {
        translatedLabel.setVisible(show);
        approvedLabel.setVisible(show);
        additionalLabel1.setVisible(show);
        additionalLabel2.setVisible(show);
        additionalLabel3.setVisible(show);
    }

    public void setProgressTexts(String translatedLabelText, String approvedLabelText) {
        translatedLabel.setText(translatedLabelText);
        translatedLabel.setIcon(IconLoader.getIcon("/icons/translated.svg", TreeCellLanguage.class));
        approvedLabel.setText(approvedLabelText);
        approvedLabel.setIcon(IconLoader.getIcon("/icons/approved.svg", TreeCellLanguage.class));
    }

    public JPanel getContent() {
        return content;
    }
}
