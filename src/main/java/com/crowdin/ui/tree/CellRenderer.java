package com.crowdin.ui.tree;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class CellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        CellData cellData = CellRenderer.getData(value);
        if (cellData == null) {
            return null;
        }

        JBLabel label = new JBLabel(cellData.getText());
        label.setIcon(cellData.getIcon());
        if (cellData.getColor() != null) {
            label.setForeground(cellData.getColor());
        }
        return FormBuilder.createFormBuilder().addComponent(label).getPanel();
    }

    public static CellData getData(Object value) {
        return CellData.class.cast(DefaultMutableTreeNode.class.cast(value).getUserObject());
    }

}
