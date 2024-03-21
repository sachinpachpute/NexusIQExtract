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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Coordinates other = (Coordinates) obj;
        return groupId.equals(other.groupId) &&
                artifactId.equals(other.artifactId) &&
                version.equals(other.version) &&
                extension.equals(other.extension);
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "format='" + format + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", extension='" + extension + '\'' +
                '}';
    }
}
