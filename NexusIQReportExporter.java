import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NexusIQReportExporter {

    public static void main(String[] args) {
        String applicationPublicId = "your_application_public_id";
        String reportId = "your_report_id";
        String url = "http://localhost:8070/api/v2/applications/" + applicationPublicId + "/reports/" + reportId + "/policy";

        try {
            String responseJson = sendGetRequest(url);
            List<PolicyDetail> policyDetails = extractPolicyDetails(responseJson);
            writeToCSV(policyDetails, "policy_details.csv");
            System.out.println("CSV file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sendGetRequest(String url) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }

    private static List<PolicyDetail> extractPolicyDetails(String responseJson) throws IOException {
        List<PolicyDetail> policyDetails = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode policiesNode = rootNode.path("policies");

        for (JsonNode policyNode : policiesNode) {
            String threatLevel = policyNode.path("threatLevel").asText();
            String policyName = policyNode.path("policyName").asText();
            JsonNode componentNode = policyNode.path("component");
            String componentDisplayName = componentNode.path("displayName").asText();
            String applicationName = componentNode.path("application").path("name").asText();
            policyDetails.add(new PolicyDetail(threatLevel, policyName, componentDisplayName, applicationName));
        }
        return policyDetails;
    }

    private static void writeToCSV(List<PolicyDetail> policyDetails, String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            writer.println("Policy Threat level,Policy Name,Component Display Name,Application Name");
            for (PolicyDetail detail : policyDetails) {
                writer.println(detail.getThreatLevel() + "," + detail.getPolicyName() + "," +
                        detail.getComponentDisplayName() + "," + detail.getApplicationName());
            }
        }
    }

    private static class PolicyDetail {
        private String threatLevel;
        private String policyName;
        private String componentDisplayName;
        private String applicationName;

        public PolicyDetail(String threatLevel, String policyName, String componentDisplayName, String applicationName) {
            this.threatLevel = threatLevel;
            this.policyName = policyName;
            this.componentDisplayName = componentDisplayName;
            this.applicationName = applicationName;
        }

        public String getThreatLevel() {
            return threatLevel;
        }

        public String getPolicyName() {
            return policyName;
        }

        public String getComponentDisplayName() {
            return componentDisplayName;
        }

        public String getApplicationName() {
            return applicationName;
        }
    }
}
