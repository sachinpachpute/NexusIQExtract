package com.vocalink.bacs;

public class SecurityIssue {
    private String source;
    private String reference;
    private String severity;
    private String threatCategory;
    private String url;

    public SecurityIssue(String source, String reference, String severity, String threatCategory, String url) {
        this.source = source;
        this.reference = reference;
        this.severity = severity;
        this.threatCategory = threatCategory;
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public String getReference() {
        return reference;
    }

    public String getSeverity() {
        return severity;
    }

    public String getThreatCategory() {
        return threatCategory;
    }

    public String getUrl() {
        return url;
    }
}
