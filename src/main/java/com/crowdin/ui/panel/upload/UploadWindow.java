package com.crowdin.ui.panel.upload;

import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;
import java.util.Optional;

public class UploadWindow implements ContentTab {
    private final JPanel panel;
    private final Tree tree = new Tree();
    private final JBLabel placeholder = new JBLabel("Tree loading", SwingConstants.CENTER);

    private DefaultMutableTreeNode selectedElement;

    public UploadWindow() {
        placeholder.setComponentStyle(UIUtil.ComponentStyle.LARGE);
        this.panel = FormBuilder
                .createFormBuilder()
                .addComponent(placeholder)
                .addComponent(new JBScrollPane(tree))
                .getPanel();

        this.tree.setCellRenderer(new CellRenderer());
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.setVisible(false);
        this.tree.addTreeSelectionListener(e ->
                Optional.ofNullable(e.getNewLeadSelectionPath())
                        .map(TreePath::getLastPathComponent)
                        .map(DefaultMutableTreeNode.class::cast)
                        .ifPresent(node -> this.selectedElement = node)
        );
    }

    @Override
    public JPanel getContent() {
        return panel;
    }

    public List<String> getSelectedFiles() {
        return FileTree.getFiles(this.selectedElement);
    }

    public void rebuildTree(String projectName, List<String> files) {
        this.selectedElement = null;
        if (files.isEmpty()) {
            tree.setVisible(false);
            placeholder.setText("No files found matching your configuration");
            placeholder.setVisible(true);
            return;
        }

        this.placeholder.setVisible(false);
        this.tree.setVisible(true);
        this.tree.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
        expandAll();
    }

    public void expandAll() {
        FileTree.expandAll(this.tree);
    }

    public void collapseAll() {
        FileTree.collapseAll(this.tree);
    }
}
