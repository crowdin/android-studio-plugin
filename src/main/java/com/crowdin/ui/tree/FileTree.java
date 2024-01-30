package com.crowdin.ui.tree;

import com.crowdin.util.FileUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.StreamSupport;

public class FileTree {

    public static DefaultMutableTreeNode buildTree(String name, List<String> files) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(name));

        List<AbstractMap.SimpleEntry<String, List<String>>> parts = files
                .stream()
                .map(f -> f.startsWith(FileUtil.PATH_SEPARATOR) ? f.substring(1) : f)
                .map(f -> {
                    List<String> fileParts = StreamSupport.stream(Paths.get(f).spliterator(), false).map(Path::toString).toList();
                    return new AbstractMap.SimpleEntry<>(f, fileParts);
                })
                .toList();

        for (AbstractMap.SimpleEntry<String, List<String>> entry : parts) {
            String filePath = entry.getKey();
            List<String> subParts = entry.getValue();
            DefaultMutableTreeNode prev = root;
            for (int j = 0; j < subParts.size(); j++) {
                for (int k = 0; k < j; k++) {
                    String parent = subParts.get(k);
                    Enumeration<TreeNode> children = prev.children();
                    while (children.hasMoreElements()) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                        CellData data = CellRenderer.getData(child);
                        if (data.getText().equals(parent)) {
                            prev = child;
                            break;
                        }
                    }
                }

                String part = subParts.get(j);

                if (j + 1 != subParts.size()) {
                    //check if folder already created
                    boolean alreadyCreated = false;
                    Enumeration<TreeNode> children = prev.children();
                    while (children.hasMoreElements()) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                        CellData data = CellRenderer.getData(child);
                        if (data.getText().equals(part)) {
                            alreadyCreated = true;
                            break;
                        }
                    }
                    if (alreadyCreated) {
                        continue;
                    }
                }

                DefaultMutableTreeNode element = j + 1 == subParts.size()
                        ? new DefaultMutableTreeNode(CellData.file(part, filePath))
                        : new DefaultMutableTreeNode(CellData.folder(part));
                prev.add(element);

                if (j + 1 == subParts.size()) {
                    //reset
                    prev = root;
                }
            }
        }

        return root;
    }

    public static List<String> getFiles(DefaultMutableTreeNode selectedElement) {
        if (selectedElement == null) {
            return Collections.emptyList();
        }

        CellData cell = CellRenderer.getData(selectedElement);

        if (cell.isRoot()) {
            //empty list to force bulk action
            return Collections.emptyList();
        }

        if (cell.isFile()) {
            return Collections.singletonList(cell.getFile());
        }

        return FileTree
                .childNodes(selectedElement, true)
                .stream()
                .map(CellRenderer::getData)
                .filter(CellData::isFile)
                .map(CellData::getFile)
                .toList();
    }

    public static List<DefaultMutableTreeNode> childNodes(DefaultMutableTreeNode parent, boolean recursive) {
        List<DefaultMutableTreeNode> res = new ArrayList<>();
        Enumeration<TreeNode> children = recursive ? parent.breadthFirstEnumeration() : parent.children();
        while (children.hasMoreElements()) {
            res.add((DefaultMutableTreeNode) children.nextElement());
        }
        return res;
    }
}
