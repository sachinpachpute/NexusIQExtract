package com.vocalink.bacs;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibraryVersionComparator {

    public static String findLatestVersion(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        // Sort the versions using custom comparator
        Collections.sort(versions, LibraryVersionComparator::compareVersions);

        // Return the last (highest) version
        return versions.get(versions.size() - 1);
    }

    public static int compareVersions(String version1, String version2) {
        // Regular expression pattern for parsing semantic versioning (semver)
        String semverPattern = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+))?$";

        // Compile the pattern
        Pattern pattern = Pattern.compile(semverPattern);

        // Match version 1 against the pattern
        Matcher matcher1 = pattern.matcher(version1);
        boolean isSemver1 = matcher1.matches();

        // Match version 2 against the pattern
        Matcher matcher2 = pattern.matcher(version2);
        boolean isSemver2 = matcher2.matches();

        // If both versions are semantic versions, compare them as such
        if (isSemver1 && isSemver2) {
            return compareSemverVersions(version1, version2);
        }

        // If one version is a semantic version and the other is not, consider the semantic version greater
        if (isSemver1) {
            return 1;
        }
        if (isSemver2) {
            return -1;
        }

        // For non-semantic versions, use lexicographical comparison
        return version1.compareTo(version2);
    }

    private static int compareSemverVersions(String version1, String version2) {
        // Your comparison logic for semantic versions goes here
        // This method should handle comparing major, minor, and patch versions,
        // as well as pre-release identifiers if present

        // For demonstration purposes, let's assume version1 is greater if it has a higher major version
        // and if major versions are equal, version1 is greater if it has a higher minor version

        // Split versions into parts
        String[] parts1 = version1.split("\\.|\\-");
        String[] parts2 = version2.split("\\.|\\-");

        // Compare major versions
        int major1 = Integer.parseInt(parts1[0]);
        int major2 = Integer.parseInt(parts2[0]);
        if (major1 != major2) {
            return Integer.compare(major1, major2);
        }

        // Compare minor versions
        int minor1 = Integer.parseInt(parts1[1]);
        int minor2 = Integer.parseInt(parts2[1]);
        if (minor1 != minor2) {
            return Integer.compare(minor1, minor2);
        }

        // Compare patch versions
        int patch1 = Integer.parseInt(parts1[2]);
        int patch2 = Integer.parseInt(parts2[2]);
        return Integer.compare(patch1, patch2);
    }

    public static void main(String[] args) {
        // Example list of version strings
        List<String> versions = List.of("1.2.3", "1.2.3-alpha", "1.2-alpha", "invalid-version", "1.2-abc");

        // Find the latest version
        String latestVersion = findLatestVersion(versions);

        // Print the latest version
        System.out.println("Latest version: " + latestVersion);
    }
}

