package com.crowdin.ui.panel.download;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public class DownloadWindow implements ContentTab {

    private JPanel panel1;
    private Tree tree1;
    private JScrollPane scrollPane;

    public DownloadWindow() {
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        tree1.setCellRenderer(new CellRenderer());
        this.setPlug("Refresh tree");
    }

    public void setPlug(String text) {
        tree1.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(CellData.root(text))));
    }

    @Override
    public JPanel getContent() {
        return panel1;
    }

    public void rebuildFileTree(String projectName, List<String> files) {
        tree1.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
    }

    public void rebuildBundlesTree(String projectName, List<Bundle> bundles) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(projectName));
        //TODO implement bundle list (probably use different form as JTree not suitable for editable nodes)
        tree1.setModel(new DefaultTreeModel(root));
    }
}
