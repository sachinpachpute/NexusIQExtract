import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class NexusIQEvaluation {

    public static void performEvaluation(String applicationId, String username, String password, ArrayList<String> componentHashes) throws IOException {
        // Nexus IQ server URL and API endpoint
        String iqServerUrl = "http://your-iq-server-url";
        String endpoint = "/api/v2/evaluation/applications/" + applicationId;

        // Prepare the request body (list of component hashes)
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("[");
        for (String componentHash : componentHashes) {
            requestBody.append("\"").append(componentHash).append("\",");
        }
        // Remove the trailing comma if there are components
        if (!componentHashes.isEmpty()) {
            requestBody.deleteCharAt(requestBody.length() - 1);
        }
        requestBody.append("]");

        // Prepare the HTTP POST request
        URL url = new URL(iqServerUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Basic " + encodeCredentials(username, password));
        conn.setDoOutput(true);

        // Write the request body to the request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response from the server
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
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
        ArrayList<String> componentHashes = new ArrayList<>();
        componentHashes.add("component-hash-1");
        componentHashes.add("component-hash-2");

        try {
            performEvaluation(applicationId, username, password, componentHashes);
        } catch (IOException e) {
            System.err.println("Failed to perform evaluation: " + e.getMessage());
        }
    }
}
