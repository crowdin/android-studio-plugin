package com.crowdin.ui.tree;

import com.crowdin.util.FileUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FileTree {

    public static DefaultMutableTreeNode buildTree(String name, List<String> files) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CellData.root(name));

        List<List<String>> parts = files
                .stream()
                .map(f -> f.startsWith(FileUtil.PATH_SEPARATOR) ? f.substring(1) : f)
                .map(f -> StreamSupport.stream(Paths.get(f).spliterator(), false).map(Path::toString).collect(Collectors.toList()))
                .collect(Collectors.toList());

        for (List<String> subParts : parts) {
            DefaultMutableTreeNode prev = root;
            for (int j = 0; j < subParts.size(); j++) {
                for (int k = 0; k < j; k++) {
                    String parent = subParts.get(k);
                    Enumeration<DefaultMutableTreeNode> children = prev.children();
                    while (children.hasMoreElements()) {
                        DefaultMutableTreeNode child = children.nextElement();
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
                    Enumeration<DefaultMutableTreeNode> children = prev.children();
                    while (children.hasMoreElements()) {
                        DefaultMutableTreeNode child = children.nextElement();
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
                        ? new DefaultMutableTreeNode(CellData.file(part))
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
}
