package com.vocalink.bacs;

public class Workflow {

    private String applicationName;
    private String dependencyName;
    private String impactedWorkflow;
    private String testCase;

    private String testCaseLink;

    public Workflow(String applicationName, String dependencyName, String impactedWorkflow, String testCase, String testCaseLink) {
        this.applicationName = applicationName;
        this.dependencyName = dependencyName;
        this.impactedWorkflow = impactedWorkflow;
        this.testCase = testCase;
        this.testCaseLink = testCaseLink;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public String getImpactedWorkflow() {
        return impactedWorkflow;
    }

    public String getTestCase() {
        return testCase;
    }

    public String getTestCaseLink() {
        return testCaseLink;
    }
}
