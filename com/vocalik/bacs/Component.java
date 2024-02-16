package com.vocalink.bacs;

import java.util.List;

public class Component {
    private int threatLevel;
    private String threatCategory;
    private String policyName;
    private String applicationName;
    private String dependency;
    private String componentDisplayName;
    private String currentVersion;
    private String latestVersion;
    private String nextVersionWithNoPolicyViolation;
    private String componentHash;
    private String componentGroupId;
    private String artifactId;
    private List<ComponentVersion> componentVersionsList;

    public Component(String applicationName, String componentDisplayName, boolean directDependency, int threatLevel, String threatCategory, String policyName, String currentVersion, String componentHash, String componentGroupId, String artifactId) {
        this.threatLevel = threatLevel;
        this.threatCategory = threatCategory;
        this.policyName = policyName;
        this.componentDisplayName = componentDisplayName;
        this.applicationName = applicationName;
        this.dependency = directDependency? "Direct":"Transitive";
        this.currentVersion = currentVersion;
        this.componentHash = componentHash;
        this.componentGroupId = componentGroupId;
        this.artifactId = artifactId;
    }

    public int getThreatLevel() {
        return threatLevel;
    }

    public String getThreatCategory() {
        return threatCategory;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getDependency() {
        return dependency;
    }

    public String getComponentDisplayName() {
        return componentDisplayName;
    }
    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getNextVersionWithNoPolicyViolation() {
        return nextVersionWithNoPolicyViolation;
    }

    public String getComponentHash() {
        return componentHash;
    }

    public String getComponentGroupId() {
        return componentGroupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public List<ComponentVersion> getComponentVersionsList() {
        return componentVersionsList;
    }

    public void setComponentVersionsList(List<ComponentVersion> componentVersionsList) {
        this.componentVersionsList = componentVersionsList;
    }
}
