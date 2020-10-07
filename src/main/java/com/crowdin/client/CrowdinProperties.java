package com.crowdin.client;

import lombok.Data;

import java.util.Map;

@Data
public class CrowdinProperties {

    private Long projectId;
    private String apiToken;
    private String baseUrl;
    private boolean disabledBranches;
    private boolean preserveHierarchy;
    private Map<String, String> sourcesWithPatterns;
    private boolean debug;
}
