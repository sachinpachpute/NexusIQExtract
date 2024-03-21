package com.vocalink.bacs;

import java.util.List;

public class PolicyViolation {
    private String policyId;
    private String policyName;
    private String policyThreatCategory;
    private int policyThreatLevel;
    private List<SecurityIssue> securityIssuesList;
    private List<Constraint> constraintViolations;
    //private String waived;

    public PolicyViolation(String policyId, String policyName, String policyThreatCategory, int policyThreatLevel) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.policyThreatCategory = policyThreatCategory;
        this.policyThreatLevel = policyThreatLevel;
        //this.waived = waived;
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyThreatCategory() {
        return policyThreatCategory;
    }

    public int getPolicyThreatLevel() {
        return policyThreatLevel;
    }

    public List<SecurityIssue> getSecurityIssuesList() {
        return securityIssuesList;
    }

    public void setSecurityIssuesList(List<SecurityIssue> securityIssuesList) {
        this.securityIssuesList = securityIssuesList;
    }

    public List<Constraint> getConstraintViolations() {
        return constraintViolations;
    }

    public void setConstraintViolations(List<Constraint> constraintViolations) {
        this.constraintViolations = constraintViolations;
    }

    /*public String getWaived() {
        return waived;
    }*/
}
