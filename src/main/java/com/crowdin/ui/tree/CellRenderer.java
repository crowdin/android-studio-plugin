package com.crowdin.ui.tree;

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

        FilesTreeItem filesTreeItem = new FilesTreeItem(cellData.getText(), cellData.getIcon());
        return filesTreeItem.getContent();
    }

    public static CellData getData(Object value) {
        return CellData.class.cast(DefaultMutableTreeNode.class.cast(value).getUserObject());
    }

}
