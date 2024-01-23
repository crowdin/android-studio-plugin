package com.crowdin.ui.panel.upload;

import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;

public class UploadWindow implements ContentTab {
    private JPanel panel1;
    private JScrollPane scrollPane;
    private Tree tree1;

    public UploadWindow() {
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        tree1.setCellRenderer(new CellRenderer());
        tree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setPlug("Refresh tree");
    }

    public void setPlug(String text) {
        tree1.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(CellData.root(text))));
    }

    @Override
    public JPanel getContent() {
        return panel1;
    }

    public void rebuildTree(String projectName, List<String> files) {
        tree1.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
    }
}
