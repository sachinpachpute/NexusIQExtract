import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApplicationLibraryExtractor {
    private static final String NEXUS_IQ_URL = "http://your_nexus_iq_server_url";
    private static final String NEXUS_RM_URL = "http://your_nexus_rm_server_url";
    private static final String IQ_USERNAME = "your_iq_username";
    private static final String IQ_PASSWORD = "your_iq_password";
    private static final String RM_USERNAME = "your_rm_username";
    private static final String RM_PASSWORD = "your_rm_password";
    private static final String CSV_FILE_PATH = "application_library_list.csv";

    public static void main(String[] args) {
        try {
            List<Application> applications = getApplicationsFromIQ();
            for (Application app : applications) {
                List<Library> libraries = getLibrariesFromRM(app.getName());
                app.setLibraries(libraries);
            }
            writeApplicationsToCSV(applications);
            System.out.println("Application and library data exported to: " + new File(CSV_FILE_PATH).getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Application> getApplicationsFromIQ() throws IOException {
        List<Application> applications = new ArrayList<>();
        URL url = new URL(NEXUS_IQ_URL + "/api/v2/applications");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthHeader(IQ_USERNAME, IQ_PASSWORD));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(reader);
            for (JsonNode node : jsonNode) {
                String appName = node.get("name").asText();
                applications.add(new Application(appName));
            }
        }
        conn.disconnect();
        return applications;
    }

    private static List<Library> getLibrariesFromRM(String applicationName) throws IOException {
        List<Library> libraries = new ArrayList<>();
        URL url = new URL(NEXUS_RM_URL + "/service/rest/v1/components?repository=releases&q=" + applicationName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthHeader(RM_USERNAME, RM_PASSWORD));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(reader).get("items");
            for (JsonNode node : jsonNode) {
                String libName = node.get("name").asText();
                String version = node.get("version").asText();
                String latestVersion = getLatestVersion(libName);
                libraries.add(new Library(libName, version, latestVersion));
            }
        }
        conn.disconnect();
        return libraries;
    }

    private static String getLatestVersion(String libName) throws IOException {
        URL url = new URL(NEXUS_RM_URL + "/service/rest/v1/search/assets/download?repository=releases&maven.groupId=" + libName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthHeader(RM_USERNAME, RM_PASSWORD));

        String latestVersion = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(reader).get("items");
            if (jsonNode.size() > 0) {
                latestVersion = jsonNode.get(0).get("version").asText();
            }
        }
        conn.disconnect();
        return latestVersion;
    }

    private static void writeApplicationsToCSV(List<Application> applications) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(CSV_FILE_PATH)))) {
            writer.println("Application Name,Library Name,Current Version,Latest Version");
            for (Application app : applications) {
                for (Library lib : app.getLibraries()) {
                    writer.println(app.getName() + "," + lib.getName() + "," + lib.getCurrentVersion() + "," + lib.getLatestVersion());
                }
            }
        }
    }

    private static String getBasicAuthHeader(String username, String password) {
        String authString = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(authString.getBytes());
    }

    private static class Application {
        private String name;
        private List<Library> libraries;

        public Application(String name) {
            this.name = name;
            this.libraries = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public List<Library> getLibraries() {
            return libraries;
        }

        public void setLibraries(List<Library> libraries) {
            this.libraries = libraries;
        }
    }

    private static class Library {
        private String name;
        private String currentVersion;
        private String latestVersion;

        public Library(String name, String currentVersion, String latestVersion) {
            this.name = name;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
        }

        public String getName() {
            return name;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public String getLatestVersion() {
            return latestVersion;
        }
    }
}
