package com.crowdin.client;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CrowdinProperties {

    private Long projectId;
    private String apiToken;
    private String baseUrl;
    private boolean disabledBranches;
    private boolean preserveHierarchy;
    private List<FileBean> files;
    private boolean debug;
    private boolean autocompletionDisabled;
    private List<String> autocompletionFileExtensions;
    private boolean importEqSuggestions;
    private boolean autoApproveImported;
    private boolean translateHidden;
}
