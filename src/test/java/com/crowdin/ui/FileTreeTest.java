package com.crowdin.ui;

import com.crowdin.ui.tree.CellData;
import com.crowdin.ui.tree.CellRenderer;
import com.crowdin.ui.tree.FileTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FileTreeTest {

    @Test
    public void buildTreeTest() {
        String name = "Test Project";
        String file1Json = Paths.get("src", "main", "file1.json").toString();
        String file2Json = Paths.get("src", "main", "file2.json").toString();
        String file3Json = Paths.get("src", "test", "file3.json").toString();
        String file4Json = Paths.get("src", "test", "file4.json").toString();
        String fileJson = Paths.get("app", "file.json").toString();
        String file5Json = Paths.get("app", "main", "file5.json").toString();
        String file6Json = Paths.get("app", "main", "folder", "file6.json").toString();

        List<String> files = Arrays.asList(
                file1Json,
                file2Json,
                file3Json,
                file4Json,
                fileJson,
                file5Json,
                file6Json
        );

        DefaultMutableTreeNode root = FileTree.buildTree(name, files);
        Assertions.assertEquals(CellRenderer.getData(root).getText(), name);

        List<DefaultMutableTreeNode> level1 = FileTree.childNodes(root, false);
        List<CellData> level1Data = level1.stream().map(CellRenderer::getData).toList();
        Assertions.assertEquals(level1Data.size(), 2);

        DefaultMutableTreeNode src = level1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("src")).findFirst().get();
        List<DefaultMutableTreeNode> level2_1 = FileTree.childNodes(src, false);
        Assertions.assertEquals(level2_1.size(), 2);

        DefaultMutableTreeNode main1 = level2_1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("main")).findFirst().get();
        List<DefaultMutableTreeNode> level3_1 = FileTree.childNodes(main1, false);
        Assertions.assertEquals(level3_1.size(), 2);

        CellData file1 = level3_1.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file1.json")).findFirst().get();
        Assertions.assertTrue(file1.isFile());
        Assertions.assertEquals(file1.getFile(), file1Json);
        CellData file2 = level3_1.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file2.json")).findFirst().get();
        Assertions.assertTrue(file2.isFile());
        Assertions.assertEquals(file2.getFile(), file2Json);

        DefaultMutableTreeNode test = level2_1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("test")).findFirst().get();
        List<DefaultMutableTreeNode> level3_2 = FileTree.childNodes(test, false);
        Assertions.assertEquals(level3_2.size(), 2);

        CellData file3 = level3_2.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file3.json")).findFirst().get();
        Assertions.assertTrue(file3.isFile());
        Assertions.assertEquals(file3.getFile(), file3Json);
        CellData file4 = level3_2.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file4.json")).findFirst().get();
        Assertions.assertTrue(file4.isFile());
        Assertions.assertEquals(file4.getFile(), file4Json);

        DefaultMutableTreeNode app = level1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("app")).findFirst().get();
        List<DefaultMutableTreeNode> level2_2 = FileTree.childNodes(app, false);
        Assertions.assertEquals(level2_2.size(), 2);

        CellData file = level2_2.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file.json")).findFirst().get();
        Assertions.assertTrue(file.isFile());
        Assertions.assertEquals(file.getFile(), fileJson);

        DefaultMutableTreeNode main2 = level2_2.stream().filter(e -> CellRenderer.getData((e)).getText().equals("main")).findFirst().get();
        List<DefaultMutableTreeNode> level3_3 = FileTree.childNodes(main2, false);
        Assertions.assertEquals(level3_3.size(), 2);

        CellData file5 = level3_3.stream().map(CellRenderer::getData).filter(e -> e.getText().equals("file5.json")).findFirst().get();
        Assertions.assertTrue(file5.isFile());
        Assertions.assertEquals(file5.getFile(), file5Json);

        DefaultMutableTreeNode folder = level3_3.stream().filter(e -> CellRenderer.getData((e)).getText().equals("folder")).findFirst().get();
        List<DefaultMutableTreeNode> level4_1 = FileTree.childNodes(folder, false);
        Assertions.assertEquals(level4_1.size(), 1);

        CellData file6 = CellRenderer.getData(level4_1.get(0));
        Assertions.assertEquals(file6.getText(), "file6.json");
        Assertions.assertTrue(file6.isFile());
        Assertions.assertEquals(file6.getFile(), file6Json);

    }

    @Test
    public void getFilesTest() {
        String file1Json = Paths.get("main", "file1.json").toString();
        String file2Json = Paths.get("main", "file2.json").toString();
        String file3Json = Paths.get("test", "file3.json").toString();

        List<String> files = Arrays.asList(
                file1Json,
                file2Json,
                file3Json
        );

        DefaultMutableTreeNode root = FileTree.buildTree("src", files);

        List<DefaultMutableTreeNode> level1 = FileTree.childNodes(root, false);
        DefaultMutableTreeNode main = level1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("main")).findFirst().get();
        DefaultMutableTreeNode test = level1.stream().filter(e -> CellRenderer.getData((e)).getText().equals("test")).findFirst().get();

        List<String> rootFiles = FileTree.getFiles(root);

        Assertions.assertEquals(rootFiles.size(), 0);

        List<String> mainFiles = FileTree.getFiles(main);

        Assertions.assertEquals(mainFiles.size(), 2);
        Assertions.assertTrue(mainFiles.contains(file1Json));
        Assertions.assertTrue(mainFiles.contains(file2Json));

        List<String> testFiles = FileTree.getFiles(test);

        Assertions.assertEquals(testFiles.size(), 1);
        Assertions.assertTrue(testFiles.contains(file3Json));
    }
}
