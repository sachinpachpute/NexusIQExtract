import java.util.List;

public class VersionUtils {

    public static String findLatestVersion(List<String> versions) {
        String latestVersion = null;
        for (String version : versions) {
            if (latestVersion == null || isNewerVersion(version, latestVersion)) {
                latestVersion = version;
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
        int minor1 = Integer.parseInt(parts1[1]);
        int minor2 = Integer.parseInt(parts2[1]);
        if (minor1 != minor2) {
            return minor1 > minor2;
        }

        // Compare patch version
        int patch1 = Integer.parseInt(parts1[2]);
        int patch2 = Integer.parseInt(parts2[2]);
        return patch1 > patch2;
    }

    public static void main(String[] args) {
        // Example list of version strings
        List<String> versions = List.of("1.2.3-RESOURCE", "1.2.3", "2.0.0", "1.5.2.RESOURCE");

        // Find the latest version
        String latestVersion = findLatestVersion(versions);
        System.out.println("Latest version: " + latestVersion); // Output: 2.0.0
    }
}
