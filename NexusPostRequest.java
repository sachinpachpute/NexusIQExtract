import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NexusPostRequest {

    public static void performEvaluation(String applicationId, String username, String password, List<ComponentVersion> componentVersions) throws IOException {
        // Nexus IQ server URL and API endpoint
        String iqServerUrl = "http://your-iq-server-url";
        String endpoint = "/api/v2/evaluation/applications/" + applicationId;

        // Prepare the JSON request body
        JsonObject requestBody = new JsonObject();
        JsonArray componentsArray = new JsonArray();
        for (ComponentVersion cv : componentVersions) {
            JsonObject componentObject = new JsonObject();
            componentObject.addProperty("hash", cv.getHash());
            componentsArray.add(componentObject);
        }
        requestBody.add("components", componentsArray);

        // Prepare the HTTP POST request
        URL url = new URL(iqServerUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Basic " + encodeCredentials(username, password));
        conn.setDoOutput(true);

        // Write the JSON request body to the request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response from the server
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response from Nexus IQ Server:");
            System.out.println(response.toString());
        }

        // Close the connection
        conn.disconnect();
    }

    private static String encodeCredentials(String username, String password) {
        String credentials = username + ":" + password;
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    public static void main(String[] args) {
        // Example data
        String applicationId = "your-application-id";
        String username = "your-username";
        String password = "your-password";
        List<ComponentVersion> componentVersions = List.of(
                new ComponentVersion("646da0e0fa6c56ff2f1b81601fb8934393718217", "1.0.0"),
                new ComponentVersion("c43f6e6bfa79b56e04a8898a923c3cf7144dd460", "2.0.0")
        );

        try {
            performEvaluation(applicationId, username, password, componentVersions);
        } catch (IOException e) {
            System.err.println("Failed to perform evaluation: " + e.getMessage());
        }
    }

    static class ComponentVersion {
        private String hash;
        private String version;

        public ComponentVersion(String hash, String version) {
            this.hash = hash;
            this.version = version;
        }

        public String getHash() {
            return hash;
        }

        public String getVersion() {
            return version;
        }
    }
}
