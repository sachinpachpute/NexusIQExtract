import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibraryVersionComparator {

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
        // Split versions into parts
        String[] parts1 = version1.split("[.-]");
        String[] parts2 = version2.split("[.-]");

        // Compare major, minor, and patch versions
        for (int i = 0; i < 3; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        // Compare pre-release versions
        if (parts1.length > 3 || parts2.length > 3) {
            // Determine maximum number of pre-release identifiers
            int maxIdentifiers = Math.max(parts1.length, parts2.length) - 3;

            for (int i = 3; i < maxIdentifiers + 3; i++) {
                // Check if both versions have pre-release identifiers at this position
                if (i < parts1.length && i < parts2.length) {
                    String identifier1 = parts1[i];
                    String identifier2 = parts2[i];
                    int comparison = identifier1.compareTo(identifier2);
                    if (comparison != 0) {
                        return comparison;
                    }
                } else if (i < parts1.length) {
                    // Version 1 has additional pre-release identifiers
                    return -1;
                } else {
                    // Version 2 has additional pre-release identifiers
                    return 1;
                }
            }
        }

        // Versions are equal up to the pre-release part
        return 0;
    }

    public static void main(String[] args) {
        // Example version strings
        String version1 = "1.2.3";
        String version2 = "1.2.3-alpha";
        String version3 = "1.2-alpha";
        String version4 = "invalid-version";
        String version5 = "1.2-abc";

        // Test the version strings
        System.out.println("Comparison of version " + version1 + " and " + version2 + ": " + compareVersions(version1, version2)); // 1
        System.out.println("Comparison of version " + version2 + " and " + version3 + ": " + compareVersions(version2, version3)); // 1
        System.out.println("Comparison of version " + version4 + " and " + version5 + ": " + compareVersions(version4, version5)); // -1
    }
}
