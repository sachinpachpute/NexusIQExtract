import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NexusIQReportExporter {

    private static final String NEXUS_IQ_URL = "https://nexus-iq.vocalink.co.uk";
    private static final String APPLICATION_PUBLIC_ID = "acsw-xval";
    private static final String REPORT_ID = "013c2f60ca4849f8813daa8473aa6431";
    private static final String CSV_FILE_PATH = "NexusIQReportExporter_list.csv";
    private static final String USERNAME = "tanisha.sethi";
    private static final String PASSWORD = "V0calink2023!";

    public static void main(String[] args) {
        try {
            String responseJson = sendGetRequest();
            List<PolicyDetail> policyDetails = extractPolicyDetails(responseJson);
            writeToCSV(policyDetails, CSV_FILE_PATH);
            System.out.println("CSV file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sendGetRequest() throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();


        try {
            URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/applications/" + APPLICATION_PUBLIC_ID + "/reports/" + REPORT_ID + "/policy");
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            String userCredentials = USERNAME + ":" + PASSWORD;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);

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
        JsonNode componentsNode = rootNode.path("components");
        String applicationName = rootNode.path("application").path("name").asText();

        for (JsonNode componentNode : componentsNode) {
            String componentDisplayName = componentNode.path("displayName").asText();
            String policyName = null;
            int threatLevel = 0;
            String threatCategory = null;

            JsonNode violationsNode = componentNode.path("violations");

            if (violationsNode.isEmpty()){
                continue;
            }
            for (JsonNode violationNode : violationsNode) {
                if (violationNode.path("policyThreatLevel").asInt() <= threatLevel){
                    continue;
                }
                threatLevel = violationNode.path("policyThreatLevel").asInt();
                policyName = violationNode.path("policyName").asText();
                threatCategory = violationNode.path("policyThreatCategory").asText();
            }

            policyDetails.add(new PolicyDetail(threatLevel, threatCategory, policyName, componentDisplayName, applicationName));
        }
        return policyDetails;
    }

    private static void writeToCSV(List<PolicyDetail> policyDetails, String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            writer.println("Threat level,Threat Category, Policy Name,Component,Application");
            for (PolicyDetail detail : policyDetails) {
                writer.println(detail.getThreatLevel() + "," + detail.getThreatCategory() + ","+ detail.getPolicyName() + "," +
                        detail.getComponentDisplayName() + "," + detail.getApplicationName());
            }
        }
    }

    private static class PolicyDetail {
        private int threatLevel;
        private String threatCategory;
        private String policyName;
        private String componentDisplayName;
        private String applicationName;

        public PolicyDetail(int threatLevel, String threatCategory, String policyName, String componentDisplayName, String applicationName) {
            this.threatLevel = threatLevel;
            this.threatCategory = threatCategory;
            this.policyName = policyName;
            this.componentDisplayName = componentDisplayName;
            this.applicationName = applicationName;
        }

        public int getThreatLevel() {
            return threatLevel;
        }

        public String getThreatCategory() {
            return threatCategory;
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
