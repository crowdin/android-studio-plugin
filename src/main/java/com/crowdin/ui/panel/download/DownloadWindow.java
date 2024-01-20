package com.crowdin.ui.panel.download;

import com.crowdin.client.CrowdinProjectCacheProvider;
import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

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

    public void rebuildTree(CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache) {
        tree1.setModel(new DefaultTreeModel(this.buildTree(crowdinProjectCache)));
    }

    private TreeNode buildTree(CrowdinProjectCacheProvider.CrowdinProjectCache crowdinProjectCache) {
        //TODO build proper tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(crowdinProjectCache.getProject().getName()));
        DefaultMutableTreeNode folder1 = new DefaultMutableTreeNode(CellData.folder("Folder 1"));
        DefaultMutableTreeNode file1 = new DefaultMutableTreeNode(CellData.file("test.json"));
        DefaultMutableTreeNode file2 = new DefaultMutableTreeNode(CellData.file("qwe.txt"));
        folder1.add(file1);
        folder1.add(file2);
        DefaultMutableTreeNode folder2 = new DefaultMutableTreeNode(CellData.folder("Folder 2"));
        DefaultMutableTreeNode file3 = new DefaultMutableTreeNode(CellData.file("app.yml"));
        folder2.add(file3);
        root.add(folder1);
        root.add(folder2);
        return root;
    }
}
