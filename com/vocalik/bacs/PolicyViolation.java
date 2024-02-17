package com.vocalink.bacs;

public class PolicyViolation {
    private String policyId;
    private String policyName;
    private String policyThreatCategory;
    private int policyThreatLevel;
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

    /*public String getWaived() {
        return waived;
    }*/
}
