package com.crowdin.ui.dialog;

import javax.swing.*;

public class ConfirmActionPanel {

    private Boolean doNotAskAgain = false;

    private JCheckBox doNotAskMeCheckBox;
    private JLabel question;
    private JPanel panel;

    public ConfirmActionPanel(String questionText) {
        question.setText(questionText);
        doNotAskMeCheckBox.addActionListener(e -> {
            doNotAskAgain = ((AbstractButton) e.getSource()).getModel().isSelected();
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    public Boolean isDoNotAskAgain() {
        return doNotAskAgain;
    }
}
