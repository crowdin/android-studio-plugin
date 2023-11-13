package com.crowdin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitUtilTest {

    @Test
    public void getCurrentBranchTest() {
        assertEquals(GitUtil.normalizeBranchName("main|1>2"), "main.1.2");
        assertEquals(GitUtil.normalizeBranchName("dev/1"), "dev.1");
        assertEquals(GitUtil.normalizeBranchName("dev\\1"), "dev.1");
        assertEquals(GitUtil.normalizeBranchName("feat:123?"), "feat.123.");
        assertEquals(GitUtil.normalizeBranchName("base*"), "base.");
        assertEquals(GitUtil.normalizeBranchName("test?\""), "test..");
        assertEquals(GitUtil.normalizeBranchName("test<"), "test.");
    }
}
