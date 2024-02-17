package com.vocalink.bacs;

public class Coordinates {
    private String format;
    private String groupId;
    private String artifactId;
    private String version;
    private String extension;

    public Coordinates(String format, String groupId, String artifactId, String version, String extension) {
        this.format = format;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.extension = extension;
    }

    public String getFormat() {
        return format;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getExtension() {
        return extension;
    }
}
