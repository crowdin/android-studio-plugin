package com.crowdin.ui;

import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class FileTreeTest {

    @Test
    public void buildTreeTest() {
        String name = "Test Project";
        List<String> files = Arrays.asList(
                Paths.get("src", "main", "file1.json").toString(),
                Paths.get("src", "main", "file2.json").toString(),
                Paths.get("src", "test", "file3.json").toString(),
                Paths.get("src", "test", "file4.json").toString(),
                Paths.get("app", "file.json").toString(),
                Paths.get("app", "main", "file5.json").toString(),
                Paths.get("app", "main", "folder", "file6.json").toString()
        );

        DefaultMutableTreeNode root = FileTree.buildTree(name, files);
        Assertions.assertEquals(CellRenderer.getData(root).getText(), name);

        List<DefaultMutableTreeNode> level1 = childNodes(root);
        List<CellData> level1Data = level1.stream().map(CellRenderer::getData).collect(Collectors.toList());
        Assertions.assertEquals(level1Data.size(), 2);

        DefaultMutableTreeNode src = level1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("src")).findFirst().get();
        List<DefaultMutableTreeNode> level2_1 = childNodes(src);
        Assertions.assertEquals(level2_1.size(), 2);

        DefaultMutableTreeNode main1 = level2_1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("main")).findFirst().get();
        List<DefaultMutableTreeNode> level3_1 = childNodes(main1);
        Assertions.assertEquals(level3_1.size(), 2);

        CellData file1 = level3_1.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file1.json")).findFirst().get();
        Assertions.assertTrue(file1.isFile());
        CellData file2 = level3_1.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file2.json")).findFirst().get();
        Assertions.assertTrue(file2.isFile());

        DefaultMutableTreeNode test = level2_1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("test")).findFirst().get();
        List<DefaultMutableTreeNode> level3_2 = childNodes(test);
        Assertions.assertEquals(level3_2.size(), 2);

        CellData file3 = level3_2.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file3.json")).findFirst().get();
        Assertions.assertTrue(file3.isFile());
        CellData file4 = level3_2.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file4.json")).findFirst().get();
        Assertions.assertTrue(file4.isFile());

        DefaultMutableTreeNode app = level1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("app")).findFirst().get();
        List<DefaultMutableTreeNode> level2_2 = childNodes(app);
        Assertions.assertEquals(level2_2.size(), 2);

        CellData file = level2_2.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file.json")).findFirst().get();
        Assertions.assertTrue(file.isFile());

        DefaultMutableTreeNode main2 = level2_2.stream().filter(e -> CellRenderer.getData((e)).getText().equals("main")).findFirst().get();
        List<DefaultMutableTreeNode> level3_3 = childNodes(main2);
        Assertions.assertEquals(level3_3.size(), 2);

        CellData file5 = level3_3.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file5.json")).findFirst().get();
        Assertions.assertTrue(file5.isFile());

        DefaultMutableTreeNode folder = level3_3.stream().filter(e -> CellRenderer.getData((e)).getText().equals("folder")).findFirst().get();
        List<DefaultMutableTreeNode> level4_1 = childNodes(folder);
        Assertions.assertEquals(level4_1.size(), 1);

        CellData file6 = CellRenderer.getData(level4_1.get(0));
        Assertions.assertEquals(file6.getText(), "file6.json");
        Assertions.assertTrue(file6.isFile());

    }

    private List<DefaultMutableTreeNode> childNodes(DefaultMutableTreeNode parent) {
        List<DefaultMutableTreeNode> res = new ArrayList<>();
        Enumeration<DefaultMutableTreeNode> children = parent.children();
        while (children.hasMoreElements()) {
            res.add(children.nextElement());
        }
        return res;
    }
}
