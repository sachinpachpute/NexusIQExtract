package com.vocalink.bacs;

public class Remediation {
    private String applicationName;
    private String releaseDelivered;
    private String cveCount;
    //private String cveAfterRemediation;

    public Remediation(String applicationName, String releaseDelivered, String cveCount) {
        this.applicationName = applicationName;
        this.releaseDelivered = releaseDelivered;
        this.cveCount = cveCount;
        //this.cveAfterRemediation = cveAfterRemediation;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getReleaseDelivered() {
        return releaseDelivered;
    }

    public String getCveCount() {
        return cveCount;
    }
}
