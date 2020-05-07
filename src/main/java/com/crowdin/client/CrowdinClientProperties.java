package com.crowdin.client;

import com.crowdin.util.PropertyUtil;

import static com.crowdin.util.PropertyUtil.PROPERTIES_FILE;

class CrowdinClientProperties {

    private static final String PROJECT_ID = "project_id";
    private static final String API_TOKEN = "api_token";
    private static final String BASE_URL = "base_url";

    private Long projectId;
    private String token;
    private String baseUrl;
    private String errorMessage;

    static CrowdinClientProperties load() {
        CrowdinClientProperties crowdinClientProperties = new CrowdinClientProperties();
        if (PropertyUtil.getCrowdinPropertyFile() == null) {
            crowdinClientProperties.errorMessage = "File '" + PROPERTIES_FILE + "' with Crowdin plugin configuration doesn't exist in project root directory";
            return crowdinClientProperties;
        }

        String projectIdentifier = PropertyUtil.getPropertyValue(PROJECT_ID);
        Long projectId = null;
        if (!"".equals(projectIdentifier)) {
            try {
                projectId = Long.valueOf(projectIdentifier);
            } catch (NumberFormatException e) {
                crowdinClientProperties.errorMessage = "Invalid project id";
            }
        } else {
            crowdinClientProperties.errorMessage = "Project id is empty";
        }
        if (crowdinClientProperties.errorMessage != null) {
            return crowdinClientProperties;
        }
        crowdinClientProperties.projectId = projectId;

        String apiToken = PropertyUtil.getPropertyValue(API_TOKEN);
        crowdinClientProperties.token = apiToken;
        if ("".equals(apiToken)) {
            crowdinClientProperties.errorMessage = "Api token is empty";
            return crowdinClientProperties;
        }

        String baseUrl = PropertyUtil.getPropertyValue(BASE_URL);
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
