package com.crowdin.client;

import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.project.Project;

import static com.crowdin.util.PropertyUtil.PROPERTIES_FILE;

class CrowdinClientProperties {

    private static final String PROJECT_ID = "project-id";
    private static final String API_TOKEN = "api-token";
    private static final String BASE_URL = "base-url";

    private Long projectId;
    private String token;
    private String baseUrl;
    private String errorMessage;

    static CrowdinClientProperties load(Project project) {
        CrowdinClientProperties crowdinClientProperties = new CrowdinClientProperties();
        if (PropertyUtil.getCrowdinPropertyFile(project) == null) {
            crowdinClientProperties.errorMessage = "File '" + PROPERTIES_FILE + "' with Crowdin plugin configuration doesn't exist in project root directory";
            return crowdinClientProperties;
        }

        String projectIdentifier = PropertyUtil.getPropertyValue(PROJECT_ID, project);
        Long projectId = null;
        if (!"".equals(projectIdentifier)) {
            try {
                projectId = Long.valueOf(projectIdentifier);
            } catch (NumberFormatException e) {
                crowdinClientProperties.errorMessage = "Project id is not a number in crowdin.properties";
            }
        } else {
            crowdinClientProperties.errorMessage = "Missing \"project_id\" property in crowdin.properties";
        }
        if (crowdinClientProperties.errorMessage != null) {
            return crowdinClientProperties;
        }
        crowdinClientProperties.projectId = projectId;

        String apiToken = PropertyUtil.getPropertyValue(API_TOKEN, project);
        crowdinClientProperties.token = apiToken;
        if ("".equals(apiToken)) {
            crowdinClientProperties.errorMessage = "Missing \"api_token\" property in crowdin.properties";
            return crowdinClientProperties;
        }

        String baseUrl = PropertyUtil.getPropertyValue(BASE_URL, project);
        if ("".equals(baseUrl)) {
            baseUrl = null;
        }
        crowdinClientProperties.baseUrl = baseUrl;

        return crowdinClientProperties;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getToken() {
        return token;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
