package com.vocalink.bacs;

public class SecurityIssue {
    private String source;
    private String reference;
    private double severity;
    private String status;
    private String url;
    private String threatCategory;
    private String cwe;

    public SecurityIssue(String source, String reference, double severity, String status, String url, String threatCategory, String cwe) {
        this.source = source;
        this.reference = reference;
        this.severity = severity;
        this.status = status;
        this.url = url;
        this.threatCategory = threatCategory;
        this.cwe = cwe;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThreatCategory() {
        return threatCategory;
    }

    public void setThreatCategory(String threatCategory) {
        this.threatCategory = threatCategory;
    }

    public String getCwe() {
        return cwe;
    }

    public void setCwe(String cwe) {
        this.cwe = cwe;
    }
}
