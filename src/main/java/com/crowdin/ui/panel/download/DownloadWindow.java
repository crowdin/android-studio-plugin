package com.crowdin.ui.panel.download;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.client.projectsgroups.model.Project;
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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import static com.crowdin.Constants.*;
import static com.crowdin.util.Util.extractOrganization;

public class DownloadWindow implements ContentTab {

    private final JPanel panel;
    private final Tree tree = new Tree();
    private boolean isBundlesMode = false;
    private DefaultMutableTreeNode selectedElement;
    private final JBLabel placeholder = new JBLabel("Tree loading", SwingConstants.CENTER);
    private final JBLabel placeholder2 = new JBLabel("", SwingConstants.CENTER);

    private final JPanel placeholderPanel = new JPanel(new BorderLayout());

    private String baseUrl;
    private Project project;

    public DownloadWindow() {
        this.placeholder.setComponentStyle(UIUtil.ComponentStyle.LARGE);
        this.placeholder2.setComponentStyle(UIUtil.ComponentStyle.LARGE);
        this.placeholder2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.placeholder2.setForeground(new Color(0, 102, 204));
        this.placeholderPanel.add(placeholder, BorderLayout.CENTER);
        this.placeholderPanel.add(placeholder2, BorderLayout.PAGE_END);

        this.panel = FormBuilder
                .createFormBuilder()
                .addComponent(placeholderPanel)
                .addComponent(new JBScrollPane(tree))
                .getPanel();

        this.tree.setCellRenderer(new CellRenderer());
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.setVisible(false);

        this.placeholder2.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                var link = buildBundleUrl();
                if (link != null) {
                    BrowserUtil.browse(link);
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
                CrowdinPanelWindowFactory.updateToolbar(BUNDLE_SETTINGS_ACTION, "Bundle settings", true, true);
            } else {
                CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);
                CrowdinPanelWindowFactory.updateToolbar(BUNDLE_SETTINGS_ACTION, "Select bundle to view settings", true, false);
            }
        });
    }

    @Override
    public JPanel getContent() {
        return this.panel;
    }

    public Bundle getSelectedBundle() {
        return Optional.ofNullable(this.selectedElement)
                .map(e -> CellRenderer.getData(e).getBundle())
                .orElse(null);
    }

    public List<String> getSelectedFiles() {
        return FileTree.getFiles(this.selectedElement);
    }

    public void rebuildFileTree(Project project, String baseUrl, List<String> files) {
        this.baseUrl = baseUrl;
        this.project = project;
        this.isBundlesMode = false;
        this.selectedElement = null;

        if (files.isEmpty()) {
            this.tree.setVisible(false);
            this.placeholder.setText("No files found matching your configuration");
            this.placeholder2.setVisible(false);
            this.placeholderPanel.setVisible(true);
            CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "Download Sources", true, false);
            CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download Translations", true, false);
            CrowdinPanelWindowFactory.updateToolbar(BUNDLE_SETTINGS_ACTION, "", false, false);
            return;
        }

        this.placeholderPanel.setVisible(false);
        this.tree.setVisible(true);

        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "Download Sources", true, true);
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download Translations", true, true);
        CrowdinPanelWindowFactory.updateToolbar(BUNDLE_SETTINGS_ACTION, "", false, false);
        this.tree.setModel(new DefaultTreeModel(FileTree.buildTree(this.project.getName(), files)));
        FileTree.expandAll(tree);
    }

    public void rebuildBundlesTree(Project project, String baseUrl, List<Bundle> bundles) {
        this.project = project;
        this.baseUrl = baseUrl;
        this.isBundlesMode = true;
        this.selectedElement = null;

        if (bundles.isEmpty()) {
            this.tree.setVisible(false);
            this.placeholder.setText("No bundles found.");
            this.placeholder2.setText("Manage bundles.");
            this.placeholder2.setVisible(true);
            this.placeholder.setVisible(true);
            this.placeholderPanel.setVisible(true);
            return;
        }

        this.placeholderPanel.setVisible(false);
        this.tree.setVisible(true);

        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_SOURCES_ACTION, "", false, false);
        CrowdinPanelWindowFactory.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);
        CrowdinPanelWindowFactory.updateToolbar(BUNDLE_SETTINGS_ACTION, "Select bundle to view settings", true, false);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(this.project.getName()));
        bundles.forEach(bundle -> root.add(new DefaultMutableTreeNode(CellData.bundle(bundle))));
        this.tree.setModel(new DefaultTreeModel(root));
        expandAll();
    }

    public void expandAll() {
        FileTree.expandAll(this.tree);
    }

    public void collapseAll() {
        FileTree.collapseAll(this.tree);
    }

    public String buildBundleUrl() {
        if (!this.isBundlesMode) {
            return null;
        }

        var bundle = this.getSelectedBundle();

        if (bundle == null) {
            if (this.baseUrl != null) {
                String organization = extractOrganization(this.baseUrl);
                return "https://" + organization + ".crowdin.com/u/projects/" + project.getId() + "/download";
            } else {
                return "https://crowdin.com/project/" + project.getIdentifier() + "/download#bundles";
            }
        }

        if (this.baseUrl != null) {
            String organization = extractOrganization(this.baseUrl);
            return "https://" + organization + ".crowdin.com/u/projects/" + project.getId() + "/translations/bundle/" + bundle.getId();
        } else {
            return "https://crowdin.com/project/" + project.getIdentifier() + "/download#bundles:" + bundle.getId();
        }
    }
}
