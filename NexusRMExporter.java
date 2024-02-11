import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class NexusRMExporter {
    private static final String NEXUS_RM_URL = "https://nexus-rm.vocalink.co.uk";

    private static final String REPOSITORY_NAME = "releases";
    private static final String USERNAME = "sachin.pachpute";
    private static final String PASSWORD = "SachShrav@404";

    private static final String CSV_FILE_PATH = "repositories_list.csv";

    public static void main(String[] args) {
        try {
            List<Component> components = getComponentsFromRepository(REPOSITORY_NAME);
            URL url = new URL(NEXUS_RM_URL + "/nexus/service/rest/v1/repositories");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            String userCredentials = USERNAME + ":" + PASSWORD;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
            conn.setRequestProperty("Authorization", basicAuth);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println("List of Repositories:");
                System.out.println(response.toString());

                String absoluteFilePath = writeResponseToCSV(response.toString());
                System.out.println("Data is exported to: " + absoluteFilePath);

                // Parse JSON response
                // Assuming JSON structure is like { "components": [ { "name": "library1", ... }, { "name": "library2", ... }, ... ] }
                /*String jsonResponse = response.toString();
                String[] libraries = jsonResponse.split("\"name\":\"");
                PrintWriter writer = new PrintWriter(new FileWriter("libraries.txt"));
                for (int i = 1; i < libraries.length; i++) {
                    String library = libraries[i].split("\"")[0];
                    writer.println(library);
                }
                writer.close();*/
            } else {
                System.out.println("Failed to fetch data list. Response code: " + responseCode);
            }
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Component> getComponentsFromRepository(String repositoryName) throws IOException {
        List<Component> components = new ArrayList<>();
        URL url = new URL(NEXUS_RM_URL + "/service/rest/v1/components?repository=" + repositoryName);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String userCredentials = USERNAME + ":" + PASSWORD;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            System.out.println("List of Repositories:");
            System.out.println(response.toString());

            String absoluteFilePath = writeResponseToCSV(response.toString());
            System.out.println("Data is exported to: " + absoluteFilePath);
        }

        return components;
    }

    private static String  writeResponseToCSV(String responseData) {
        File csvFile = new File(CSV_FILE_PATH);
        try (PrintWriter writer = new PrintWriter(new BufferedWriter((new FileWriter(csvFile))))){
            //Write response data to CSV file
            writer.println(responseData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFile.getAbsolutePath();
    }

    private static class Component {
        private String applicationName;
        private String componentName;
        private String currentVersion;
        private String latestVersion;

        public Component(String applicationName, String componentName, String currentVersion, String latestVersion) {
            this.applicationName = applicationName;
            this.componentName = componentName;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
        }

        public String toCSVString() {
            return String.join(",", applicationName, componentName, currentVersion, latestVersion);
        }
    }
}
