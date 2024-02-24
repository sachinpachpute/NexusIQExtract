package com.vocalink.bacs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String CSV_FILE_PATH = "VulnerabilityExport_2.csv";

    /*private static final String APPLICATION_PUBLIC_ID = "psw_refresh";
    private static final String APPLICATION_INTERNAL_ID = "78b741479d47430898a26b8139adb48b";
    private static final String REPORT_ID = "54b967c921224f27897ee5567a3f98e5";*/



    public static void main(String[] args) {
        try {
            String reportJson = getApplicationDependencyDataByApplicationPublicIdAndReportId();
            List<ApplicationDependency> applicationDependencies = parseReportJsonToFormAListOfApplicationDependencies(reportJson);
            writeToCSV(applicationDependencies, CSV_FILE_PATH);
            System.out.println("CSV file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getApplicationDependencyDataByApplicationPublicIdAndReportId() throws IOException {

        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/applications/" + APPLICATION_PUBLIC_ID + "/reports/" + REPORT_ID + "/policy");

        String jsonResponse = connectToNexusIQ(urlObj);

        return jsonResponse;
    }

    private static List<ApplicationDependency> parseReportJsonToFormAListOfApplicationDependencies(String reportJson) throws IOException {
        List<ApplicationDependency> applicationDependencies = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(reportJson);
        JsonNode dependenciesNode = rootNode.path("components");
        String applicationName = rootNode.path("application").path("name").asText();
        List <PolicyViolation> allPolicyViolations = new LinkedList<>();
        PolicyViolation highestPolicyViolation = null;
        Coordinates coordinates = null;
        List<Component> otherAvailableVersions;

        for (JsonNode dependencyNode : dependenciesNode) {
            String dependencyName = dependencyNode.path("displayName").asText();
            String dependencyPackageUrl = dependencyNode.path("packageUrl").asText();
            boolean isDirectDependency = dependencyNode.path("dependencyData").path("directDependency").asBoolean();
            boolean isInnerSource = dependencyNode.path("dependencyData").path("innerSource").asBoolean();
            String format = dependencyNode.path("componentIdentifier").path("format").asText();
            String groupId = dependencyNode.path("componentIdentifier").path("coordinates").path("groupId").asText();
            String artifactId = dependencyNode.path("componentIdentifier").path("coordinates").path("artifactId").asText();
            String version = dependencyNode.path("componentIdentifier").path("coordinates").path("version").asText();
            String extension = dependencyNode.path("componentIdentifier").path("coordinates").path("extension").asText();

            coordinates = new Coordinates(format, groupId, artifactId, version, extension);
            //System.out.println(groupId+":"+artifactId);

            if(groupId.isEmpty() || artifactId.isEmpty()){
                continue;
            }

            /*if(!coordinates.equals(new Coordinates("maven", "com.thoughtworks.xstream", "javax.servlet.jsp.jstl", "1.4.11.1", "jar"))){
                continue;
            }*/

            String policyId;
            String policyName;
            int policyThreatLevel = 1;
            String policyThreatCategory;
            int highestIdentifiedPolicyThreatLevel = 1;

            JsonNode policyViolationsNode = dependencyNode.path("violations");

            if (policyViolationsNode.isEmpty()){
                continue;
            }
            for (JsonNode policyViolationNode : policyViolationsNode) {
                policyId = policyViolationNode.path("policyId").asText();
                policyName = policyViolationNode.path("policyName").asText();
                policyThreatLevel = policyViolationNode.path("policyThreatLevel").asInt();
                policyThreatCategory = policyViolationNode.path("policyThreatCategory").asText();

                PolicyViolation pv = new PolicyViolation(policyId, policyName, policyThreatCategory, policyThreatLevel);
                if(allPolicyViolations.isEmpty()){
                    highestPolicyViolation = pv;
                    highestIdentifiedPolicyThreatLevel = pv.getPolicyThreatLevel();
                }
                allPolicyViolations.add(pv);

                if (policyViolationNode.path("policyThreatLevel").asInt() <= highestIdentifiedPolicyThreatLevel){
                    continue;
                } else {
                    highestPolicyViolation = pv;
                    highestIdentifiedPolicyThreatLevel = pv.getPolicyThreatLevel();
                }
            }

            ApplicationDependency applicationDependency = new ApplicationDependency(applicationName, dependencyName, dependencyPackageUrl, isDirectDependency, isInnerSource, coordinates);
            if (!allPolicyViolations.isEmpty()){
                applicationDependency.setAllPolicyViolations(allPolicyViolations);
            }

            if(highestPolicyViolation != null){
                applicationDependency.setHighestPolicyViolation(highestPolicyViolation);
            }

            //Get Next Version with No Policy Violations from Nexus IQ
            populateNextVersionWithNoPolicyViolation(applicationDependency);

            otherAvailableVersions = getOtherVersionsOfTheComponent(applicationDependency);
            applicationDependency.setOtherAvailableVersions(otherAvailableVersions);

            applicationDependencies.add(applicationDependency);
        }
        return applicationDependencies;
    }

    private static void populateNextVersionWithNoPolicyViolation(ApplicationDependency applicationDependency) throws IOException {

        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/components/remediation/application/" + APPLICATION_INTERNAL_ID);
        String userCredentials = IQ_USERNAME + ":" + IQ_PASSWORD;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

        // Construct the request body JSON string
        StringBuilder requestBodyBuilder = new StringBuilder();

            requestBodyBuilder.append("    {\n");
            requestBodyBuilder.append("      \"componentIdentifier\": {\n");
            requestBodyBuilder.append("        \"format\": \"maven\",\n");
            requestBodyBuilder.append("        \"coordinates\": {\n");
            requestBodyBuilder.append("          \"artifactId\": \"" + applicationDependency.getCoordinates().getArtifactId() + "\",\n");
            requestBodyBuilder.append("          \"extension\": \"" + applicationDependency.getCoordinates().getExtension() + "\",\n");
            requestBodyBuilder.append("          \"groupId\": \"" + applicationDependency.getCoordinates().getGroupId() + "\",\n");
            requestBodyBuilder.append("          \"version\": \"" + applicationDependency.getCoordinates().getVersion() + "\"\n");
            requestBodyBuilder.append("        }\n");
            requestBodyBuilder.append("      }\n");
            requestBodyBuilder.append("    }");


        String requestBody = requestBodyBuilder.toString();

        // Prepare the HTTP POST request
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);

        //System.out.println(requestBody.toString());

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
            //System.out.println("Response from Nexus IQ Server:");
            //System.out.println(response.toString());
        }

        // Close the connection
        conn.disconnect();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.toString());
        JsonNode versionChanges = rootNode.path("remediation").path("versionChanges");

        String nextVersionWithNoViolations = null;
        String nextVersionWithNoViolationsWithDependencies = null;

        if(!versionChanges.isEmpty()){
            for(JsonNode versionChange : versionChanges){
                if(versionChange.has("type") && versionChange.path("type").asText().equalsIgnoreCase("next-no-violations")){
                    nextVersionWithNoViolations = versionChange.path("data").path("component")
                            .path("componentIdentifier").path("coordinates").path("version").asText();
                } else if(versionChange.has("type") && versionChange.path("type").asText().equalsIgnoreCase("next-no-violations-with-dependencies")){
                    nextVersionWithNoViolationsWithDependencies = versionChange.path("data").path("component")
                            .path("componentIdentifier").path("coordinates").path("version").asText();
                }
            }
        }
        applicationDependency.setNextVersionWithNoPolicyViolation(nextVersionWithNoViolations);
        applicationDependency.setNextVersionWithNoPolicyViolationWithDependencies(nextVersionWithNoViolationsWithDependencies);
    }

    private static List<Component> getOtherVersionsOfTheComponent(ApplicationDependency applicationDependency) throws IOException {
        {
            URL urlObj = new URL(NEXUS_RM_URL + "/nexus/service/rest/v1/search/assets?group=" + applicationDependency.getCoordinates().getGroupId() + "&name=" + applicationDependency.getCoordinates().getArtifactId() + "&maven.extension=jar&maven.classifier&repository=central&sort=version");
            //System.out.println(urlObj.toString());

            String jsonResponseComponentVersions = connectToNexusRM(urlObj);

            //System.out.println("***** This is for component "+ component.getComponentGroupId()+":"+ component.getArtifactId());
            List <Component> components = parseJsonToFormAListOfOtherVersionsOfTheComponent(jsonResponseComponentVersions, applicationDependency);

            if(!components.isEmpty()){
                //populateEachComponentWithSecurityViolationData(components);
                populateEachComponentWithPolicyViolationAndSecurityIssuesData(components, applicationDependency);
            }
            return components;
        }
    }

    private static void populateEachComponentWithPolicyViolationAndSecurityIssuesData(List<Component> components, ApplicationDependency applicationDependency) throws IOException {
        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/evaluation/applications/" + APPLICATION_INTERNAL_ID );
        String userCredentials = IQ_USERNAME + ":" + IQ_PASSWORD;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

        // Construct the request body JSON string
        StringBuilder requestBodyBuilder = new StringBuilder();

        requestBodyBuilder.append("{\n");
        requestBodyBuilder.append("  \"components\": [\n");
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            requestBodyBuilder.append("    {\n");
            requestBodyBuilder.append("      \"componentIdentifier\": {\n");
            requestBodyBuilder.append("        \"format\": \"maven\",\n");
            requestBodyBuilder.append("        \"coordinates\": {\n");
            requestBodyBuilder.append("          \"groupId\": \"" + component.getCoordinates().getGroupId() + "\",\n");
            requestBodyBuilder.append("          \"artifactId\": \"" + component.getCoordinates().getArtifactId() + "\",\n");
            requestBodyBuilder.append("          \"version\": \"" + component.getCoordinates().getVersion() + "\",\n");
            requestBodyBuilder.append("          \"extension\": \"" + component.getCoordinates().getExtension() + "\"\n");
            requestBodyBuilder.append("        }\n");
            requestBodyBuilder.append("      }\n");
            requestBodyBuilder.append("    }");
            if (i < components.size() - 1) {
                requestBodyBuilder.append(",");
            }
            requestBodyBuilder.append("\n");
        }
        requestBodyBuilder.append("  ]\n");
        requestBodyBuilder.append("}");

        String requestBody = requestBodyBuilder.toString();

        // Prepare the HTTP POST request
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);

        //System.out.println(requestBody.toString());

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
            //System.out.println("Response from Nexus IQ Server:");
            //System.out.println(response.toString());
        }

        // Close the connection
        conn.disconnect();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.toString());
        String resultsUrl = rootNode.path("resultsUrl").asText();

        try{
            //Wait for 2 seconds because report generally is not ready to be accessed immediately and you get 404 error in that case
            Thread.sleep(1000);
            if(checkIfEvaluatioResultIsReady(resultsUrl)){
                String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                populatePolicyEvaluationResultData(components, jsonResponse, applicationDependency);
            } else {
                Thread.sleep(1000);
                if(checkIfEvaluatioResultIsReady(resultsUrl)){
                    String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                    populatePolicyEvaluationResultData(components, jsonResponse, applicationDependency);
                } else {
                    populatePolicyEvaluationResultData(components, null, applicationDependency);
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

    private static void populatePolicyEvaluationResultData(List<Component> components, String jsonResponse, ApplicationDependency applicationDependency) throws IOException{
        //System.out.println(jsonResponse);
        if(jsonResponse == null){
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultsNode = rootNode.path("results");

        String policyId;
        String policyName;
        int policyThreatLevel = 1;
        int highestIdentifiedPolicyThreatLevel = 1;
        String policyThreatCategory;
        String threatCategory;

        String source;
        String reference;
        String severity;
        String status;
        String url;
        String cwe;

        StringBuilder stringBuilder = new StringBuilder();

        for (Component component : components) {
            for (JsonNode resultNode : resultsNode) {
                JsonNode componentIdentifier = resultNode.path("component").path("componentIdentifier");
                String format = componentIdentifier.path("format").asText();
                String groupId = componentIdentifier.path("coordinates").path("groupId").asText();
                String artifactId = componentIdentifier.path("coordinates").path("artifactId").asText();
                String version = componentIdentifier.path("coordinates").path("version").asText();
                String extension = componentIdentifier.path("coordinates").path("extension").asText();

                List <PolicyViolation> allPolicyViolations = new LinkedList<>();
                List <SecurityIssue> allSecurityIssues = new LinkedList<>();
                PolicyViolation highestPolicyViolation = null;

                Coordinates responseCoordinates = new Coordinates(format, groupId, artifactId, version, extension);

                if(responseCoordinates.equals(component.getCoordinates())){
                    if(resultNode.has("policyData") && !resultNode.path("policyData").path("policyViolations").isEmpty()){
                        JsonNode policyViolations = resultNode.path("policyData").path("policyViolations");
                        for (JsonNode policyViolationNode : policyViolations) {
                            policyId = policyViolationNode.path("policyId").asText();
                            policyName = policyViolationNode.path("policyName").asText();
                            policyThreatLevel = policyViolationNode.path("threatLevel").asInt();
                            policyThreatCategory = "Not Available";

                            PolicyViolation pv = new PolicyViolation(policyId, policyName, policyThreatCategory, policyThreatLevel);
                            if(allPolicyViolations.isEmpty()){
                                highestPolicyViolation = pv;
                                highestIdentifiedPolicyThreatLevel = pv.getPolicyThreatLevel();
                            }
                            allPolicyViolations.add(pv);

                            if (policyViolationNode.path("policyThreatLevel").asInt() <= highestIdentifiedPolicyThreatLevel){
                                continue;
                            } else {
                                highestPolicyViolation = pv;
                                highestIdentifiedPolicyThreatLevel = pv.getPolicyThreatLevel();
                            }
                        }
                        component.setAllPolicyViolations(allPolicyViolations);
                        component.setHighestPolicyViolation(highestPolicyViolation);
                    } else {
                        stringBuilder.append(version + "; ");
                        //applicationDependency.setNextVersionWithNoPolicyViolation(version);
                    }
                    if(resultNode.has("securityData") && !resultNode.path("securityData").path("securityIssues").isEmpty()){
                        JsonNode securityIssues = resultNode.path("securityData").path("securityIssues");
                        for (JsonNode securityIssueNode : securityIssues) {
                            source = securityIssueNode.path("source").asText();
                            reference = securityIssueNode.path("reference").asText();
                            severity = securityIssueNode.path("severity").asText();
                            status = securityIssueNode.path("status").asText();
                            url = securityIssueNode.path("url").asText();
                            threatCategory = securityIssueNode.path("threatCategory").asText();
                            cwe = securityIssueNode.path("cwe").asText();

                            SecurityIssue securityIssue = new SecurityIssue(source, reference, severity, status, url, threatCategory, cwe);
                            allSecurityIssues.add(securityIssue);
                        }
                        component.setAllSecurityIssues(allSecurityIssues);
                    } else {
                        applicationDependency.setNextVersionWithNoSecurityIssues(version);
                    }
                }
            }
        }
        applicationDependency.setMoreVersionsWithNoPolicyViolation(stringBuilder.toString());
    }

    private static List<Component> parseJsonToFormAListOfOtherVersionsOfTheComponent(String responseJson, ApplicationDependency applicationDependency) throws IOException {
        //System.out.println(responseJson);
        List<Component> components = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode componentsNode = rootNode.path("items");
        List <String> versionsList = new LinkedList<>();
        boolean isFirstComponent = false;

        for (JsonNode componentNode : componentsNode) {
            //JsonNode assetsNode = componentNode.path("assets");
            String donloadPackageUrl = componentNode.path("downloadUrl").asText();
            String format = componentNode.path("format").asText();
            String groupId = componentNode.path("maven2").path("groupId").asText();
            String artifactId = componentNode.path("maven2").path("artifactId").asText();
            String version = componentNode.path("maven2").path("version").asText();
            String extension = componentNode.path("maven2").path("extension").asText();

            if(!versionsList.contains(version) ){
                Coordinates coordinates = new Coordinates(format, groupId, artifactId, version, extension);
                Component component = new Component(donloadPackageUrl, coordinates);
                components.add(component);
                versionsList.add(version);
                if(isFirstComponent == false){
                    isFirstComponent = true;
                    component.setLatestAvailableVersion(true);
                    applicationDependency.setHighestAvailableVersion(version);
                }
            }
        }
        //Update latest versions
        //markLatestVersion(components);
        return components;
    }

    /*private static boolean isValidSemanticPattern(String version) {
        // Regular expression pattern for semantic versioning (semver) with optional pre-release and build metadata
        String semverPattern = "^(?:0|1\\.0(?:\\.0)?(?:-[A-Za-z]+(?:-[A-Za-z0-9]+)?)?|2\\.6(?:\\.0)?(?:-[A-Za-z]+(?:-[A-Za-z0-9]+)?)?|3\\.(?:0|1\\.1|4)(?:\\.(?:0|1))?((?:-[A-Za-z]+(?:-[A-Za-z0-9]+)?)|(?:\\.[A-Za-z0-9]+(?:_[A-Za-z0-9]+)?(?:-[A-Za-z0-9]+)?))?(?:\\.[A-Za-z0-9]+(?:_[A-Za-z0-9]+)?(?:-[A-Za-z0-9]+)?)?|5\\.(?:2\\.5(?:\\.[A-Za-z0-9]+)?|4\\.17(?:\\.Final)?|20\\.30?|2[012]?\\.30?))$";
        // Compile the pattern
        Pattern pattern = Pattern.compile(semverPattern);
        // Match the version string against the pattern
        Matcher matcher = pattern.matcher(version);
        // Return true if the version string matches the pattern, else false
        return matcher.matches();
    }*/

    private static void markLatestVersion(List<Component> components) {
        //Find latest version
        String latestVersion = VersionUtils.findLatestVersion(components);

        //String latestVersion = LibraryVersionComparator.findLatestVersion(components);

        //Update isLatestVersion property for the component with the latest version
        for (Component component : components){
            if(component.getCoordinates().getVersion().equals(latestVersion)){
                component.setLatestAvailableVersion(true);
            } else {
                component.setLatestAvailableVersion(false);
            }
        }
    }

    private static void writeToCSV(List<ApplicationDependency> applicationDependencies, String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            writer.println("Application, " +
                    "Component, Dependency, " +
                    "Threat level, Policy Name, " +
                    "Associated CVEs, Current Version, " +
                    "Highest Available Version, " +
                    "Next Version With No Policy Violation, " +
                    "Next Version With No Policy Violation including Dependencies, " +
                    "More Versions With No Policy Violation");
            for (ApplicationDependency detail : applicationDependencies) {

                System.out.println(detail.getDependencyName());

                //If there is no version available without any policy violations then mention what is the threat level of the policy if we upgrade to the latest available version
                /*int policyThreatLevelForLatestAvailableVersion = 0;
                if (detail.getNextVersionWithNoPolicyViolation() == null){
                    if(!detail.getOtherAvailableVersions().isEmpty() && detail.getOtherAvailableVersions().get(0).getHighestPolicyViolation() !=null){
                        policyThreatLevelForLatestAvailableVersion = detail.getOtherAvailableVersions().get(0).getHighestPolicyViolation().getPolicyThreatLevel();
                    }
                }*/

                StringBuilder cveCvssList = new StringBuilder();
                List<SecurityIssue>  securityIssues;
                //Iterate through all versions of this dependencies
                for(Component comp : detail.getOtherAvailableVersions()){
                    //Pick the currently used version
                    if(comp.getCoordinates().equals(detail.getCoordinates())){
                        //Form a string of all 'CVE (CVSS)' for this dependency
                        securityIssues = comp.getAllSecurityIssues();
                        if(securityIssues!=null && !securityIssues.isEmpty()){
                            for(SecurityIssue cve : securityIssues){
                                cveCvssList.append(cve.getReference())
                                        .append(" (CVSS:")
                                        .append(cve.getSeverity())
                                        .append(");  ");
                            }
                        }
                    }
                }

                writer.println(detail.getApplicationName() + "," + detail.getDependencyName() + "," + (detail.isDirectDependency()? "Direct": "Transitive")
                        + ","+ detail.getHighestPolicyViolation().getPolicyThreatLevel() + "," + detail.getHighestPolicyViolation().getPolicyName()
                        + "," + cveCvssList + "," + detail.getCoordinates().getVersion() + "," + detail.getHighestAvailableVersion()
                        + "," + detail.getNextVersionWithNoPolicyViolation()
                        + "," + detail.getNextVersionWithNoPolicyViolationWithDependencies()
                        /*+ "," + (detail.getNextVersionWithNoPolicyViolation()==null?"Not Available (Latest available version has policy violation " +
                        "with Threat Level " + policyThreatLevelForLatestAvailableVersion:detail.getNextVersionWithNoPolicyViolation())*/
                        + "," + detail.getMoreVersionsWithNoPolicyViolation());
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
