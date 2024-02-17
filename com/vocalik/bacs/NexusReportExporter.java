package com.vocalink.bacs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NexusReportExporter {

    private static final String NEXUS_IQ_URL = "https://nexus-iq.vocalink.co.uk";
    private static final String IQ_USERNAME = "tanisha.sethi";
    private static final String IQ_PASSWORD = "V0calink2023!";

    private static final String NEXUS_RM_URL = "https://nexus-rm.vocalink.co.uk";
    private static final String RM_USERNAME = "sachin.pachpute";
    private static final String RM_PASSWORD = "SachShrav@404";

    private static final String APPLICATION_PUBLIC_ID = "acsw-xval";
    private static final String APPLICATION_INTERNAL_ID = "9fdcfa10d35a4f109e006a7afe1d3519";
    private static final String REPORT_ID = "013c2f60ca4849f8813daa8473aa6431";
    private static final String CSV_FILE_PATH = "NexusIQReportExporter_list.csv";

    public static void main(String[] args) {
        try {
            String responseJson = getPolicyViolationsByReportRestAPI();
            List<Component> components = extractPolicyDetails(responseJson);
            writeToCSV(components, CSV_FILE_PATH);
            System.out.println("CSV file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPolicyViolationsByReportRestAPI() throws IOException {

        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/applications/" + APPLICATION_PUBLIC_ID + "/reports/" + REPORT_ID + "/policy");

        String jsonResponse = connectToNexusIQ(urlObj);

        return jsonResponse;
    }

    private static List<Component> extractPolicyDetails(String responseJson) throws IOException {
        List<Component> components = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode componentsNode = rootNode.path("components");
        String applicationName = rootNode.path("application").path("name").asText();

        for (JsonNode componentNode : componentsNode) {
            String componentDisplayName = componentNode.path("displayName").asText();
            String componentHash = componentNode.path("hash").asText();
            String componentGroupId = componentNode.path("componentIdentifier").path("coordinates").path("groupId").asText();
            String componetArtifactId = componentNode.path("componentIdentifier").path("coordinates").path("artifactId").asText();
            String currentVersion = componentNode.path("componentIdentifier").path("coordinates").path("version").asText();
            boolean directDependency = componentNode.path("dependencyData").path("directDependency").asBoolean();
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

            Component component = new Component(applicationName, componentDisplayName, directDependency, threatLevel, threatCategory, policyName, currentVersion, componentHash, componentGroupId, componetArtifactId);

            component.setComponentVersionsList(getObjList_SameComponentDifferentVersions(component));
            components.add(component);
        }
        return components;
    }

    private static List<ComponentVersion> getObjList_SameComponentDifferentVersions(Component component) throws IOException {
        {
            URL urlObj = new URL(NEXUS_RM_URL + "/nexus/service/rest/v1/search?group=" + component.getComponentGroupId() + "&name=" + component.getArtifactId() + "&sort=version");

            String jsonResponse = connectToNexusRM(urlObj);

            //System.out.println("***** This is for component "+ component.getComponentGroupId()+":"+ component.getArtifactId());

            List <ComponentVersion> componentVersions = getComponentVersionObjectList(jsonResponse);

            if(!componentVersions.isEmpty()){
                evaluateComponentVersionsForPolicyViolation(componentVersions);
            }
            return componentVersions;
        }
    }

    private static void evaluateComponentVersionsForPolicyViolation(List<ComponentVersion> componentVersions) throws IOException {
        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/evaluation/applications/" + APPLICATION_INTERNAL_ID );
        String userCredentials = IQ_USERNAME + ":" + IQ_PASSWORD;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

        // Prepare the JSON request body
        JsonObject requestBody = new JsonObject();
        JsonArray componentsArray = new JsonArray();
        for (ComponentVersion cv : componentVersions) {
            JsonObject componentHashObject = new JsonObject();
            componentHashObject.addProperty("hash", cv.getHash());
            componentsArray.add(componentHashObject);
        }

        requestBody.add("components", componentsArray);

        // Prepare the HTTP POST request
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);

        System.out.println(requestBody.toString());

        // Write the JSON request body to the request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        // Read the response from the server
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {

            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response from Nexus IQ Server:");
            System.out.println(response.toString());
        }

        // Close the connection
        conn.disconnect();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.toString());
        String resultsUrl = rootNode.path("resultsUrl").asText();

        try{
            //Wait for 2 seconds because report generally is not ready to be accessed immediately and you get 404 error in that case
            //Thread.sleep(2000);
            if(checkIfEvaluatioResultIsReady(resultsUrl)){
                String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                populateEvaluationResultData(componentVersions, jsonResponse);
            } else {
                Thread.sleep(1000);
                if(checkIfEvaluatioResultIsReady(resultsUrl)){
                    String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                    populateEvaluationResultData(componentVersions, jsonResponse);
                } else {
                    populateEvaluationResultData(componentVersions, null);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkIfEvaluatioResultIsReady(String resultsUrl) throws IOException{
        URL urlObj = new URL(NEXUS_IQ_URL + "/" + resultsUrl);

        // Open a connection
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

        // Set request method to GET
        connection.setRequestMethod("GET");
        String userCredentials = IQ_USERNAME + ":" + IQ_PASSWORD;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);

        // Get the HTTP response code
        int responseCode = connection.getResponseCode();

        // Check if the response code indicates success (200 OK)
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // The report is ready
            return true;
        } else {
            // The report is not ready or there was an error
            return false;
        }
    }

    private static void populateEvaluationResultData(List<ComponentVersion> componentVersions, String jsonResponse) throws IOException{
        if(jsonResponse == null){
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultsNode = rootNode.path("results");

        String policyName = null;
        int threatLevel = 0;
        String threatCategory = null;

        for (ComponentVersion componentVersion : componentVersions) {
            for (JsonNode resultNode : resultsNode) {
                String nodeHash = resultNode.path("component").path("hash").asText();
                if(nodeHash.equalsIgnoreCase(componentVersion.getHash())){
                    if(resultNode.has("policyData") && !resultNode.path("policyData").path("policyViolations").isEmpty()){
                        JsonNode policyViolations = resultNode.path("policyData").path("policyViolations");
                        for (JsonNode policyViolation : policyViolations) {
                            if (policyViolation.path("threatLevel").asInt() <= threatLevel){
                                continue;
                            }
                            threatLevel = policyViolation.path("threatLevel").asInt();
                            policyName = policyViolation.path("policyName").asText();
                            //constraintName =
                        }
                    }
                }
            }
        }
    }

    private static List<ComponentVersion> getComponentVersionObjectList(String responseJson) throws IOException {
        List<ComponentVersion> componentVersions = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode componentsNode = rootNode.path("items");
        List <String> versionsList = new LinkedList<>();

        for (JsonNode componentNode : componentsNode) {
            JsonNode assetsNode = componentNode.path("assets");
            String version = null;
            String hash = null;
            boolean isLatestVersion = false;

            for (JsonNode assetNode : assetsNode) {
                String extension = assetNode.path("maven2").path("extension").asText();
                String classifier = assetNode.path("maven2").path("classifier").asText();

                if(classifier.isEmpty() && extension.equalsIgnoreCase("jar")){
                    version = assetNode.path("maven2").path("version").asText();
                    hash = assetNode.path("checksum").path("sha1").asText();

                    if(!versionsList.contains(version) && isValidSemanticPattern(version)){
                        ComponentVersion componentVersion = new ComponentVersion(hash, version, isLatestVersion);
                        componentVersions.add(componentVersion);
                        versionsList.add(version);
                    }
                }
            }
        }
        //Update latest versions
        markLatestVersion(componentVersions);
        return componentVersions;
    }

    private static boolean isValidSemanticPattern(String version) {
        // Regular expression pattern for semantic versioning (semver) with optional pre-release and build metadata
        String semverPattern = "^(?:0|1\\.0(?:\\.0)?(?:-[A-Za-z]+(?:-[A-Za-z0-9]+)?)?|2\\.6(?:\\.0)?(?:-[A-Za-z]+(?:-[A-Za-z0-9]+)?)?|3\\.(?:0|1\\.1|4)(?:\\.(?:0|1))?((?:-[A-Za-z]+(?:-[A-Za-z0-9]+)?)|(?:\\.[A-Za-z0-9]+(?:_[A-Za-z0-9]+)?(?:-[A-Za-z0-9]+)?))?(?:\\.[A-Za-z0-9]+(?:_[A-Za-z0-9]+)?(?:-[A-Za-z0-9]+)?)?|5\\.(?:2\\.5(?:\\.[A-Za-z0-9]+)?|4\\.17(?:\\.Final)?|20\\.30?|2[012]?\\.30?))$";
        // Compile the pattern
        Pattern pattern = Pattern.compile(semverPattern);
        // Match the version string against the pattern
        Matcher matcher = pattern.matcher(version);
        // Return true if the version string matches the pattern, else false
        return matcher.matches();
    }

    private static void markLatestVersion(List<ComponentVersion> componentVersions) {
        //Find latest version
        String latestVersion = VersionUtils.findLatestVersion(componentVersions);

        //Update isLatestVersion property for the component with the latest version
        for (ComponentVersion componentVersion : componentVersions){
            if(componentVersion.getVersion().equals(latestVersion)){
                componentVersion.setLatestVersion(true);
            } else {
                componentVersion.setLatestVersion(false);
            }
        }
    }

    private static void writeToCSV(List<Component> components, String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            writer.println("Application, Component, Dependency, Threat level,Threat Category, Policy Name, Current Version, Component Hash, Component Group ID");
            for (Component detail : components) {
                writer.println(detail.getApplicationName() + "," + detail.getComponentDisplayName() + "," + detail.getDependency() + ","+ detail.getThreatLevel() + "," +
                        detail.getThreatCategory() + "," + detail.getPolicyName() + "," + detail.getCurrentVersion() + "," + detail.getComponentHash() + "," + detail.getComponentGroupId());
            }
        }
    }

    private static String connectToNexusIQ(URL urlObj) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {

            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            String userCredentials = IQ_USERNAME + ":" + IQ_PASSWORD;
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

    private static String connectToNexusRM(URL urlObj) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {

            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            String userCredentials = RM_USERNAME + ":" + RM_PASSWORD;
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
}
