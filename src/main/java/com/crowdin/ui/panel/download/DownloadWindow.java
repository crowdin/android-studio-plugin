package com.crowdin.ui.panel.download;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.panel.CrowdinPanelWindowFactory;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ide.BrowserUtil;
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
    private final JBLabel placeholder = new JBLabel("Tree loading", SwingConstants.CENTER);

    public DownloadWindow() {
        this.placeholder.setComponentStyle(UIUtil.ComponentStyle.LARGE);
        this.panel = FormBuilder
                .createFormBuilder()
                .addComponent(placeholder)
                .addComponent(new JBScrollPane(tree))
                .getPanel();

        this.tree.setCellRenderer(new CellRenderer());
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.setVisible(false);

        this.tree.addMouseListener(new MouseAdapter() {
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

        this.tree.addTreeSelectionListener(e -> {
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
        this.isBundlesMode = false;
        this.selectedElement = null;
        if (files.isEmpty()) {
            tree.setVisible(false);
            placeholder.setText("No files found");
            placeholder.setVisible(true);
            return;
        }
        this.placeholder.setVisible(false);
        this.tree.setVisible(true);

        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "Download Sources", true, true);
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download Translations", true, true);
        this.tree.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
        FileTree.expandAll(tree);
    }

    public void rebuildBundlesTree(String projectName, List<Bundle> bundles, String bundleInfoUrl) {
        this.isBundlesMode = true;
        this.selectedElement = null;
        this.placeholder.setVisible(false);
        this.tree.setVisible(true);

        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "", false, false);
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(projectName));
        bundles.forEach(bundle -> root.add(new DefaultMutableTreeNode(CellData.bundle(bundle))));
        if (bundles.isEmpty()) {
            root.add(new DefaultMutableTreeNode(CellData.link("Check how to create bundle", bundleInfoUrl)));
        }
        this.tree.setModel(new DefaultTreeModel(root));
        expandAll();
    }

    public void expandAll() {
        FileTree.expandAll(this.tree);
    }

    public void collapseAll() {
        FileTree.collapseAll(this.tree);
    }
}
