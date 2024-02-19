package com.vocalink.bacs;

import java.util.List;

public class ApplicationDependency {

    private String applicationName;
    private String dependencyName;
    private String dependencyPackageUrl;
    private boolean isDirectDependency;
    private boolean isInnerSource;
    private String nextVersionWithNoPolicyViolation;
    private String nextVersionWithNoSecurityIssues;
    private String highestAvailableVersion;
    private List <PolicyViolation> allPolicyViolations;
    private PolicyViolation highestPolicyViolation;
    private Coordinates coordinates;
    private List<Component> otherAvailableVersions;

    public ApplicationDependency(String applicationName, String dependencyName, String dependencyPackageUrl, boolean isDirectDependency, boolean isInnerSource, Coordinates coordinates) {
        this.applicationName = applicationName;
        this.dependencyName = dependencyName;
        this.dependencyPackageUrl = dependencyPackageUrl;
        this.isDirectDependency = isDirectDependency;
        this.isInnerSource = isInnerSource;
        this.coordinates = coordinates;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public void setDependencyName(String dependencyName) {
        this.dependencyName = dependencyName;
    }

    public String getDependencyPackageUrl() {
        return dependencyPackageUrl;
    }

    public void setDependencyPackageUrl(String dependencyPackageUrl) {
        this.dependencyPackageUrl = dependencyPackageUrl;
    }

    public boolean getIsDirectDependency() {
        return isDirectDependency;
    }

    public void setIsDirectDependency(boolean isDirectDependency) {
        this.isDirectDependency = isDirectDependency;
    }

    public boolean getIsInnerSource() {
        return isInnerSource;
    }

    public void setIsInnerSource(boolean isInnerSource) {
        this.isInnerSource = isInnerSource;
    }

    public String getNextVersionWithNoPolicyViolation() {
        return nextVersionWithNoPolicyViolation;
    }

    public void setNextVersionWithNoPolicyViolation(String nextVersionWithNoPolicyViolation) {
        this.nextVersionWithNoPolicyViolation = nextVersionWithNoPolicyViolation;
    }

    public String getHighestAvailableVersion() {
        return highestAvailableVersion;
    }

    public void setHighestAvailableVersion(String highestAvailableVersion) {
        this.highestAvailableVersion = highestAvailableVersion;
    }

    public List<PolicyViolation> getAllPolicyViolations() {
        return allPolicyViolations;
    }

    public void setAllPolicyViolations(List<PolicyViolation> policyViolations) {
        this.allPolicyViolations = policyViolations;
    }

    public PolicyViolation getHighestPolicyViolation() {
        return highestPolicyViolation;
    }

    public void setHighestPolicyViolation(PolicyViolation highestPolicyViolation) {
        this.highestPolicyViolation = highestPolicyViolation;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isDirectDependency() {
        return isDirectDependency;
    }

    public void setDirectDependency(boolean directDependency) {
        isDirectDependency = directDependency;
    }

    public List<Component> getOtherAvailableVersions() {
        return otherAvailableVersions;
    }

    public void setOtherAvailableVersions(List<Component> otherAvailableVersions) {
        this.otherAvailableVersions = otherAvailableVersions;
    }

    public String getNextVersionWithNoSecurityIssues() {
        return nextVersionWithNoSecurityIssues;
    }

    public void setNextVersionWithNoSecurityIssues(String nextVersionWithNoSecurityIssues) {
        this.nextVersionWithNoSecurityIssues = nextVersionWithNoSecurityIssues;
    }
}
