package com.vocalink.bacs;

import java.util.List;

public class ComponentVersion {
    private String hash;
    private String version;

    boolean isLatestVersion;
    List<SecurityIssue> policyViolations;

    public ComponentVersion(String hash, String version, boolean isLatestVersion) {
        this.hash = hash;
        this.version = version;
        this.isLatestVersion = isLatestVersion;
    }

    public String getHash() {
        return hash;
    }

    public String getVersion() {
        return version;
    }

    public boolean isLatestVersion() {
        return isLatestVersion;
    }

    public void setLatestVersion(boolean latestVersion) {
        isLatestVersion = latestVersion;
    }

    public List<SecurityIssue> getPolicyViolations() {
        return policyViolations;
    }

    public void setPolicyViolations(List<SecurityIssue> policyViolations) {
        this.policyViolations = policyViolations;
    }
}
