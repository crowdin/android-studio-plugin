package com.crowdin.client;

public class BranchInfo {

    private final String name;
    private final String title;

    public BranchInfo(String name, String title) {
        this.name = name;
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }
}
