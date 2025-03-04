package com.crowdin.client.config;

import java.util.List;
import java.util.Objects;

public class FileBean {

    private String source;
    private String translation;
    private List<String> excludedTargetLanguages;
    private List<String> labels;
    private Boolean cleanupMode;
    private Boolean updateStrings;
    private Integer escapeSpecialCharacters;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public List<String> getExcludedTargetLanguages() {
        return excludedTargetLanguages;
    }

    public void setExcludedTargetLanguages(List<String> excludedTargetLanguages) {
        this.excludedTargetLanguages = excludedTargetLanguages;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Boolean getCleanupMode() {
        return cleanupMode;
    }

    public void setCleanupMode(Boolean cleanupMode) {
        this.cleanupMode = cleanupMode;
    }

    public Boolean getUpdateStrings() {
        return updateStrings;
    }

    public void setUpdateStrings(Boolean updateStrings) {
        this.updateStrings = updateStrings;
    }

    public Integer getEscapeSpecialCharacters() {
        return escapeSpecialCharacters;
    }

    public void setEscapeSpecialCharacters(Integer escapeSpecialCharacters) {
        this.escapeSpecialCharacters = escapeSpecialCharacters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileBean fileBean = (FileBean) o;
        return Objects.equals(source, fileBean.source) && Objects.equals(translation, fileBean.translation) && Objects.equals(excludedTargetLanguages, fileBean.excludedTargetLanguages) && Objects.equals(labels, fileBean.labels) && Objects.equals(cleanupMode, fileBean.cleanupMode) && Objects.equals(updateStrings, fileBean.updateStrings) && Objects.equals(escapeSpecialCharacters, fileBean.escapeSpecialCharacters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, translation, excludedTargetLanguages, labels, cleanupMode, updateStrings, escapeSpecialCharacters);
    }

    @Override
    public String toString() {
        return "FileBean{" +
                "source='" + source + '\'' +
                ", translation='" + translation + '\'' +
                ", excludedTargetLanguages=" + excludedTargetLanguages +
                ", labels=" + labels +
                ", cleanupMode=" + cleanupMode +
                ", updateStrings=" + updateStrings +
                ", escapeSpecialCharacters=" + escapeSpecialCharacters +
                '}';
    }
}
