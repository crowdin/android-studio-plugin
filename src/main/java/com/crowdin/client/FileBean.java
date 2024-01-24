package com.crowdin.client;

import lombok.Data;

import java.util.List;

@Data
public class FileBean {

    private String source;
    private String translation;
    private List<String> excludedTargetLanguages;
    private List<String> labels;
    private Boolean cleanupMode;
    private Boolean updateStrings;
}
