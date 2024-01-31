package com.crowdin.ui.panel.download;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import static com.crowdin.Constants.DOWNLOAD_SOURCES_ACTION;
import static com.crowdin.Constants.DOWNLOAD_TRANSLATIONS_ACTION;

public class DownloadWindow implements ContentTab {

    private JPanel panel1;
    private Tree tree1;
    private JScrollPane scrollPane;
    private boolean isBundlesMode = false;
    private DefaultMutableTreeNode selectedElement;

    public DownloadWindow() {
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        tree1.setCellRenderer(new CellRenderer());
        tree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setPlug("Refresh tree");

        tree1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = tree1.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree1.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() == 2) {
                        Optional
                                .ofNullable(selPath.getLastPathComponent())
                                .filter(DefaultMutableTreeNode.class::isInstance)
                                .map(CellRenderer::getData)
                                .filter(CellData::isLink)
                                .ifPresent(cell -> BrowserUtil.browse(cell.getLink()));
                    }
                }
            }
        });

        tree1.addTreeSelectionListener(e -> {
            Optional<DefaultMutableTreeNode> selectedNode = Optional.ofNullable(e.getNewLeadSelectionPath())
                    .map(TreePath::getLastPathComponent)
                    .map(DefaultMutableTreeNode.class::cast);

            if (selectedNode.isEmpty()) {
                return;
            }

            this.selectedElement = selectedNode.get();

            if (!this.isBundlesMode) {
                return;
            }

            CellData cell = CellRenderer.getData(this.selectedElement);

            if (cell.isBundle()) {
                CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download bundle", true, true);
            } else {
                CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);
            }
        });
    }

    public void setPlug(String text) {
        tree1.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(CellData.root(text))));
    }

    @Override
    public JPanel getContent() {
        return panel1;
    }

    public Bundle getSelectedBundle() {
        return CellRenderer.getData(this.selectedElement).getBundle();
    }

    public List<String> getSelectedFiles() {
        return FileTree.getFiles(this.selectedElement);
    }

    public void rebuildFileTree(String projectName, List<String> files) {
        isBundlesMode = false;
        this.selectedElement = null;
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "Download Sources", true, true);
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download Translations", true, true);
        tree1.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
        FileTree.expandAll(tree1);
    }

    public void rebuildBundlesTree(String projectName, List<Bundle> bundles, String bundleInfoUrl) {
        isBundlesMode = true;
        this.selectedElement = null;
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "", false, false);
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(projectName));
        bundles.forEach(bundle -> root.add(new DefaultMutableTreeNode(CellData.bundle(bundle))));
        if (bundles.isEmpty()) {
            root.add(new DefaultMutableTreeNode(CellData.link("Check how to create bundle", bundleInfoUrl)));
        }
        tree1.setModel(new DefaultTreeModel(root));
        expandAll();
    }

    public void expandAll() {
        FileTree.expandAll(tree1);
    }

    public void collapseAll() {
        FileTree.collapseAll(tree1);
    }
}
