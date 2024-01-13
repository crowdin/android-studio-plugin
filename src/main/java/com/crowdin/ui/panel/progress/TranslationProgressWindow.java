package com.crowdin.ui.panel.progress;

import com.crowdin.client.translationstatus.model.FileProgress;
import com.crowdin.client.translationstatus.model.LanguageProgress;
import com.crowdin.ui.panel.ContentTab;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;
import lombok.Data;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TranslationProgressWindow implements ContentTab {
    private JPanel panel1;
    private Tree tree1;
    private JLabel translatedTip;
    private JLabel approvedTip;
    private JScrollPane scrollPane;

    private boolean groupByFiles = false;

    private String projectName;
    private Map<LanguageProgress, List<FileProgress>> progressData;
    private Map<Long, String> fileNames;
    private Map<String, String> languageNames;

    public TranslationProgressWindow() {
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        translatedTip.setIcon(IconLoader.getIcon("/icons/translated.svg", this.getClass()));
        approvedTip.setIcon(IconLoader.getIcon("/icons/approved.svg", this.getClass()));
        this.setPlug("Refresh data");
        tree1.setCellRenderer(new TranslationProgressCellRenderer());
    }

    @Override
    public JPanel getContent() {
        return panel1;
    }

    public boolean isGroupByFiles() {
        return groupByFiles;
    }

    public void setGroupByFiles(boolean groupByFiles) {
        this.groupByFiles = groupByFiles;
    }

    public void setData(String projectName, Map<LanguageProgress, List<FileProgress>> progressData, Map<Long, String> fileNames, Map<String, String> languageNames) {
        this.projectName = projectName;
        this.progressData = progressData;
        this.fileNames = fileNames;
        this.languageNames = languageNames;
    }

    public void rebuildTree() {
        tree1.setModel(new DefaultTreeModel(buildTree()));
        for (int i = 0; i < tree1.getRowCount(); i++) {
            tree1.expandRow(i);
        }
    }

    public void setPlug(String text) {
        tree1.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(new TranslationProgressCellRenderer.CellData(IconLoader.getIcon("/icons/icon.svg", this.getClass()), text))));
    }

    private DefaultMutableTreeNode buildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new TranslationProgressCellRenderer.CellData(IconLoader.getIcon("/icons/icon.png", this.getClass()), projectName));
        List<LanguageProgress> sortedLanguageProgresses = progressData.keySet().stream()
            .sorted(Comparator.comparing(langProgress -> languageNames.get(langProgress.getLanguageId())))
            .collect(Collectors.toList());
        if (groupByFiles) {
            Map<String, DefaultMutableTreeNode> fileGroups = new TreeMap<>();
            for (LanguageProgress langProgress : sortedLanguageProgresses) {
                String languageName = languageNames.get(langProgress.getLanguageId());
                for (FileProgress fileProgress : progressData.get(langProgress)) {
                    String fileName = fileNames.get(fileProgress.getFileId());
                    if (fileName == null) {
                        continue;
                    }
                    fileGroups.putIfAbsent(fileName, new DefaultMutableTreeNode(new TranslationProgressCellRenderer.CellData(AllIcons.Actions.Annotate, fileName)));
                    DefaultMutableTreeNode fileNode = fileGroups.get(fileName);
                    fileNode.add(new DefaultMutableTreeNode(new TranslationProgressCellRenderer.CellData(languageName,
                        fileProgress.getTranslationProgress() + "%", fileProgress.getApprovalProgress() + "%")));
                }
            }
            System.out.println(fileGroups);
            for (DefaultMutableTreeNode fileNode : fileGroups.values()) {
                root.add(fileNode);
            }
        } else {
            for (LanguageProgress langProgress : sortedLanguageProgresses) {
                String languageName = languageNames.get(langProgress.getLanguageId());
                DefaultMutableTreeNode langNode = new DefaultMutableTreeNode(
                    new TranslationProgressCellRenderer.CellData(languageName,
                        langProgress.getTranslationProgress() + "%", langProgress.getApprovalProgress() + "%"));
                progressData.get(langProgress).stream()
                    .filter(fileProgress -> fileNames.containsKey(fileProgress.getFileId()))
                    .sorted(Comparator.comparing(l -> fileNames.get(l.getFileId())))
                    .map(fileProgress -> new DefaultMutableTreeNode(new TranslationProgressCellRenderer.CellData(AllIcons.Actions.Annotate, fileNames.get(fileProgress.getFileId()),
                        fileProgress.getTranslationProgress() + "%", fileProgress.getApprovalProgress() + "%")))
                    .forEach(langNode::add);
                root.add(langNode);
            }
        }
        return root;
    }

    public static class TranslationProgressCellRenderer extends DefaultTreeCellRenderer {

        @Data
        public static class CellData {
            private Icon icon;
            private String text;
            private String translatedProgressText;
            private String approvedProgressText;

            public CellData(String text) {
                this.text = text;
            }

            public CellData(Icon icon, String text) {
                this.icon = icon;
                this.text = text;
            }

            public CellData(String text, String translatedProgressText, String approvedProgressText) {
                this.text = text;
                this.translatedProgressText = translatedProgressText;
                this.approvedProgressText = approvedProgressText;
            }

            public CellData(Icon icon, String text, String translatedProgressText, String approvedProgressText) {
                this.icon = icon;
                this.text = text;
                this.translatedProgressText = translatedProgressText;
                this.approvedProgressText = approvedProgressText;
            }
        }


        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            CellData cellData = retrieveData(value);
            if (cellData == null) {
                return null;
            }

            TreeCellLanguage cell = new TreeCellLanguage(cellData.getText());
            if (cellData.getIcon() != null) {
                cell.setIcon(cellData.getIcon());
            }
            if (cellData.getTranslatedProgressText() != null || cellData.getApprovedProgressText() != null) {
                cell.setProgressTexts(cellData.getTranslatedProgressText(), cellData.getApprovedProgressText());
                cell.showProgress(true);
            } else {
                cell.showProgress(false);
            }
            return cell.getContent();
        }

        private CellData retrieveData(Object value) {
            return (CellData) ((DefaultMutableTreeNode) value).getUserObject();
        }
    }
}
