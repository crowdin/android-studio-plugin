package com.crowdin.ui.panel.upload;

import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.FormBuilder;

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

    private DefaultMutableTreeNode selectedElement;

    public UploadWindow() {
        this.panel = FormBuilder
                .createFormBuilder()
                .addComponent(new JBScrollPane(tree))
                .getPanel();

        tree.setCellRenderer(new CellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setPlug("Refresh tree");
        tree.addTreeSelectionListener(e ->
                Optional.ofNullable(e.getNewLeadSelectionPath())
                        .map(TreePath::getLastPathComponent)
                        .map(DefaultMutableTreeNode.class::cast)
                        .ifPresent(node -> this.selectedElement = node)
        );
    }

    public void setPlug(String text) {
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(CellData.root(text))));
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
        tree.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
        expandAll();
    }

    public void expandAll() {
        FileTree.expandAll(tree);
    }

    public void collapseAll() {
        FileTree.collapseAll(tree);
    }
}
