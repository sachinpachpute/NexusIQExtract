package com.vocalink.bacs;

import java.util.List;

public class Component {
    //private String name;
    private String downloadPackageUrl;
    Coordinates coordinates;
    private List<PolicyViolation> allPolicyViolations;
    private PolicyViolation highestPolicyViolation;
    private List<SecurityIssue> allSecurityIssues;
    boolean isLatestAvailableVersion = false;

    public Component(String downloadPackageUrl, Coordinates coordinates) {
        //this.name = name;
        this.downloadPackageUrl = downloadPackageUrl;
        this.coordinates = coordinates;
    }

    /*public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }*/

    public String getDownloadPackageUrl() {
        return downloadPackageUrl;
    }

    public void setDownloadPackageUrl(String downloadPackageUrl) {
        this.downloadPackageUrl = downloadPackageUrl;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public List<PolicyViolation> getAllPolicyViolations() {
        return allPolicyViolations;
    }

    public void setAllPolicyViolations(List<PolicyViolation> allPolicyViolations) {
        this.allPolicyViolations = allPolicyViolations;
    }

    public PolicyViolation getHighestPolicyViolation() {
        return highestPolicyViolation;
    }

    public void setHighestPolicyViolation(PolicyViolation highestPolicyViolation) {
        this.highestPolicyViolation = highestPolicyViolation;
    }
    public boolean isLatestAvailableVersion() {
        return isLatestAvailableVersion;
    }

    public void setLatestAvailableVersion(boolean latestAvailableVersion) {
        isLatestAvailableVersion = latestAvailableVersion;
    }

    public void setAllSecurityIssues(List<SecurityIssue> allSecurityIssues) {
        this.allSecurityIssues = allSecurityIssues;
    }

    public List<SecurityIssue> getAllSecurityIssues() {
        return allSecurityIssues;
    }
}
