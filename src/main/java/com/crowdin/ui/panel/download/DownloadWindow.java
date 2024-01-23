package com.crowdin.ui.panel.download;

import com.crowdin.client.bundles.model.Bundle;
import com.crowdin.ui.panel.ContentTab;
import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import com.intellij.ide.ActivityTracker;
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
import java.util.List;
import java.util.Optional;

import static com.crowdin.Constants.DOWNLOAD_SOURCES_ACTION;
import static com.crowdin.Constants.DOWNLOAD_TOOLBAR_ID;
import static com.crowdin.Constants.DOWNLOAD_TRANSLATIONS_ACTION;

public class DownloadWindow implements ContentTab {

    private JPanel panel1;
    private Tree tree1;
    private JScrollPane scrollPane;
    private boolean isBundlesMode = false;
    private Bundle selectedBundle;

    public DownloadWindow() {
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        tree1.setCellRenderer(new CellRenderer());
        tree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setPlug("Refresh tree");
        tree1.addTreeSelectionListener(e -> {
            this.selectedBundle = null;
            if (!isBundlesMode) {
                return;
            }

            Optional<CellData> selectedNode = Optional.ofNullable(e.getNewLeadSelectionPath())
                    .map(TreePath::getLastPathComponent)
                    .map(CellRenderer::getData);

            if (!selectedNode.isPresent()) {
                return;
            }

            if (selectedNode.get().isBundle()) {
                this.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download bundle", true, true);
                this.selectedBundle = selectedNode.get().getBundle();
            } else {
                this.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);
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
        return selectedBundle;
    }

    public void rebuildFileTree(String projectName, List<String> files) {
        isBundlesMode = false;
        this.updateToolbar(DOWNLOAD_SOURCES_ACTION, "Download Sources", true, true);
        this.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Download Translations", true, true);
        tree1.setModel(new DefaultTreeModel(FileTree.buildTree(projectName, files)));
    }

    public void rebuildBundlesTree(String projectName, List<Bundle> bundles) {
        isBundlesMode = true;
        this.updateToolbar(DOWNLOAD_SOURCES_ACTION, "", false, false);
        this.updateToolbar(DOWNLOAD_TRANSLATIONS_ACTION, "Select bundle to download", true, false);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(projectName));
        bundles.forEach(bundle -> root.add(new DefaultMutableTreeNode(CellData.bundle(bundle))));
        tree1.setModel(new DefaultTreeModel(root));
    }

    private void updateToolbar(String actionId, String text, boolean visible, boolean enabled) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        Presentation presentation = new Presentation();
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
        presentation.setText(text);
        action.update(AnActionEvent.createFromDataContext(DOWNLOAD_TOOLBAR_ID, presentation, DataContext.EMPTY_CONTEXT));
        ActivityTracker.getInstance().inc();
    }
}
