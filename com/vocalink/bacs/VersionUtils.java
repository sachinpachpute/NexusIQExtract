package com.vocalink.bacs;

import java.util.List;

public class VersionUtils {

    public static String findLatestVersion(List<Component> components) {

        String latestVersion = null;
        for (Component component : components) {
            //System.out.println("comparing "+componentVersion.getVersion()+" with "+latestVersion);

            if (latestVersion == null || LibraryVersionComparator.compareVersions(component.getCoordinates().getVersion(), latestVersion)==1) {
                latestVersion = component.getCoordinates().getVersion();
            }
        }
        return latestVersion;
    }

    private static boolean isNewerVersion(String version1, String version2) {
        // Remove any additional strings at the end of the version (e.g., "-RESOURCE")
        version1 = version1.replaceAll("[.-][^.-]+$", "");
        version2 = version2.replaceAll("[.-][^.-]+$", "");

        // Split versions into parts
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        // Compare major version
        int major1 = Integer.parseInt(parts1[0]);
        int major2 = Integer.parseInt(parts2[0]);
        if (major1 != major2) {
            return major1 > major2;
        }

       // Compare minor version
        int minor1 = parts1.length > 1 ? Integer.parseInt(parts1[1].replaceAll("[^\\d]","")) : 0;
        int minor2 = parts2.length > 1 ? Integer.parseInt(parts2[1].replaceAll("[^\\d]","")) : 0;
        if (minor1 != minor2) {
            return minor1 > minor2;
        }

        // Compare patch version
        int patch1 = parts1.length > 2 ? Integer.parseInt(parts1[2].replaceAll("[^\\d]","")) : 0;
        int patch2 = parts2.length > 2 ? Integer.parseInt(parts2[2].replaceAll("[^\\d]","")) : 0;
        return patch1 > patch2;
    }

//    public static void main(String[] args) {
//        // Example list of version strings
//        List<String> versions = List.of("1.2.3-RESOURCE", "1.2.3", "2.0.0", "1.5.2.RESOURCE");
//
//        // Find the latest version
//        String latestVersion = findLatestVersion(versions);
//        System.out.println("Latest version: " + latestVersion); // Output: 2.0.0
//    }
}
