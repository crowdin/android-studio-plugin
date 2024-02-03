package com.crowdin.ui.panel.download;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.FormBuilder;

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

    private final JPanel panel;
    private final Tree tree = new Tree();
    private boolean isBundlesMode = false;
    private DefaultMutableTreeNode selectedElement;

    public DownloadWindow() {
        this.panel = FormBuilder
                .createFormBuilder()
                .addComponent(new JBScrollPane(tree))
                .getPanel();

        tree.setCellRenderer(new CellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setPlug("Refresh tree");

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
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

        tree.addTreeSelectionListener(e -> {
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
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(CellData.root(text))));
    }

    @Override
    public JPanel getContent() {
        return this.panel;
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
        tree.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
        FileTree.expandAll(tree);
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
        tree.setModel(new DefaultTreeModel(root));
        expandAll();
    }

    public void expandAll() {
        FileTree.expandAll(tree);
    }

    public void collapseAll() {
        FileTree.collapseAll(tree);
    }
}
