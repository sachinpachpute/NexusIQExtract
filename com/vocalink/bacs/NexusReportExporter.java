package com.vocalink.bacs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*import java.net.URI;
import java.net.URISyntaxException;*/


public class NexusReportExporter {

    private static final String NEXUS_IQ_URL = "https://nexus-iq.vocalink.co.uk";
    private static final String IQ_USERNAME = "sachin.pachpute";
    private static final String IQ_PASSWORD = "V0calink2024!";

    private static final String NEXUS_RM_URL = "https://nexus-rm.vocalink.co.uk";
    private static final String RM_USERNAME = "sachin.pachpute";
    private static final String RM_PASSWORD = "SachShrav@404";

    /*private static final String RM_USERNAME = "tanisha.sethi";
    private static final String RM_PASSWORD = "Apr@123456789";*/

    private static Date currentDate = new Date();
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    static String formattedDateTime = dateFormat.format(currentDate);
    private static final String CSV_FILE_PATH = "VulnerabilityExport_"+formattedDateTime+".csv";

    //private static final String APPLICATION_PUBLIC_ID = "acsw-xval";
    //private static final String APPLICATION_INTERNAL_ID = "9fdcfa10d35a4f109e006a7afe1d3519";
    //private static final String REPORT_ID = "013c2f60ca4849f8813daa8473aa6431";


    /*private static final String APPLICATION_PUBLIC_ID = "psw_refresh";
    private static final String APPLICATION_INTERNAL_ID = "78b741479d47430898a26b8139adb48b";
    private static final String REPORT_ID = "54b967c921224f27897ee5567a3f98e5";*/



    public static void main(String[] args) {
        try {
            Map<String, String> applications = Configuration.getApplications();
            Workbook workbook = WorkbookFactory.create(true);

            /*if (new File(CSV_FILE_PATH).exists()) {
                workbook = WorkbookFactory.create(new FileInputStream(CSV_FILE_PATH));
            } else {
                workbook = new XSSFWorkbook(); // Create a new workbook if it doesn't exist
            }*/

            // Create a new worksheet with the specified name
            Sheet sheet = workbook.createSheet("Catalogue");

            // Write the common heading row
            Row headingRow = sheet.createRow(0);
            String[] columnNames = {"Application", "Component", "Dependency", "Threat level", "Policy Name", "Constraint Name",
                    "Constraint Reason", "Current Version", "Highest Available Version",
                    "Next Version With No Policy Violation",
                    "Next Version With No Policy Violation including Dependencies",
                    "More Versions With No Policy Violation", "CVE Count", "Impacted Workflow", "QA test cases", "Test case link"};
            for (int i = 0; i < columnNames.length; i++) {
                Cell cell = headingRow.createCell(i);
                cell.setCellValue(columnNames[i]);
            }

            List<Workflow> workflowData = getWorkflowDataFromConfluence();

            //createSummaryWorksheet(CSV_FILE_PATH, applications);
            for (Map.Entry<String, String > entry : applications.entrySet()){
                String id = entry.getKey();
                String publicId = entry.getValue();
                String reportId = null;

                //Get the latest reports ID. We will use release report but there is an option to choose other stages like build and develop
                reportId = getReportIdForGivenApplication(id, "release");
                if (reportId == null){
                    reportId = getReportIdForGivenApplication(id, "develop");
                }
                if(reportId !=null & !id.isEmpty()){
                    String reportJson = getApplicationDependencyDataByApplicationPublicIdAndReportId(publicId, reportId);
                    List<ApplicationDependency> applicationDependencies = parseReportJsonToFormAListOfApplicationDependencies(reportJson, workflowData);
                    if (applicationDependencies != null && applicationDependencies.size() == 0){
                        reportId = getReportIdForGivenApplication(id, "develop");
                        if(reportId !=null & !id.isEmpty()){
                            reportJson = getApplicationDependencyDataByApplicationPublicIdAndReportId(publicId, reportId);
                            applicationDependencies = parseReportJsonToFormAListOfApplicationDependencies(reportJson, workflowData);
                        }
                    }

                    //writeToExcel(applicationDependencies, CSV_FILE_PATH, publicId);
                    writeToExcel(applicationDependencies, workbook, CSV_FILE_PATH);
                }
            }

            // Create a new worksheet with the specified name
            Sheet sheet1 = workbook.createSheet("RemediationData");

            // Write the common heading row
            Row headingRow1 = sheet1.createRow(0);
            String[] columnNames1 = {"Application", "Release Delivered", "CVE Count"};
            for (int i = 0; i < columnNames.length; i++) {
                Cell cell = headingRow.createCell(i);
                cell.setCellValue(columnNames[i]);
            }

            List<Remediation> remediationData = getRemediationDataFromConfluence();

            writeToExcelRemediationData(remediationData, workbook, CSV_FILE_PATH);

            //writeToCSV(applicationDependencies, CSV_FILE_PATH);
            System.out.println("CSV file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Workflow> getWorkflowDataFromConfluence() throws IOException {
        String baseUrl = "https://confluence.vocalink.co.uk"; // Replace with your Confluence base URL
        String username = "sachin.pachpute"; // Replace with your Confluence username
        String password = "SachShrav@404"; // Replace with your Confluence password
        String pageId = "229413820"; // Replace with the actual page ID
        List<Workflow> workflows = null;

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(baseUrl + "/rest/api/content/" + pageId + "?expand=body.view");
            request.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));

            HttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            JSONObject json = new JSONObject(responseBody);
            JSONObject view = json.getJSONObject("body").getJSONObject("view");
            String tableHtml = view.getString("value");

            workflows = new ArrayList<>();

            // Parse table data and populate Workflow objects
            Document doc = Jsoup.parse(tableHtml);
            Element table = doc.select("table").first();
            Elements rows = table.select("tr");

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() == 5) { // Assuming each row has 4 cells representing Application, Component, Workflows, Test Cases
                    String application = cells.get(0).text();
                    String appCode = cells.get(1).text();
                    String component = cells.get(2).text();
                    String workflowsText = cells.get(3).text();
                    String testCasesText = cells.get(4).text();
                    String testCasesLink = extractHyperlinks(cells.get(4));
                    //String tempVariable = "https://mastercardgbr-my.sharepoint.com/personal/keerthana_ravikumar_mastercard_com/Documents/QA Test Cases/"+appCode+"/"+application+"_"+component+".xlsx";
                    //tempVariable = encodeFilePath(tempVariable);

                    // Create a new Workflow object and add it to the list
                    Workflow workflow = new Workflow(application, component, workflowsText, testCasesText, testCasesLink);
                    workflows.add(workflow);
                }
            }

            /*for (Workflow workflow : workflows){
                System.out.println(workflow.getApplicationName()+ ":"+workflow.getDependencyName()+ ":"+workflow.getImpactedWorkflow()+":"+workflow.getTestCase());
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workflows;
    }

    /*public static String encodeFilePath(String filePath) {
        try {
            URI uri = new URI(null, null, filePath, null);
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            // Handle URISyntaxException
            e.printStackTrace();
            return null;
        }
    }*/

    private static List<Remediation> getRemediationDataFromConfluence() throws IOException {
        String baseUrl = "https://confluence.vocalink.co.uk"; // Replace with your Confluence base URL
        String username = "sachin.pachpute"; // Replace with your Confluence username
        String password = "SachShrav@404"; // Replace with your Confluence password
        String pageId = "244387455"; // Replace with the actual page ID
        List<Remediation> remediationDataList = null;

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(baseUrl + "/rest/api/content/" + pageId + "?expand=body.view");
            request.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));

            HttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            JSONObject json = new JSONObject(responseBody);
            JSONObject view = json.getJSONObject("body").getJSONObject("view");
            String tableHtml = view.getString("value");

            remediationDataList = new ArrayList<>();

            // Parse table data and populate Workflow objects
            Document doc = Jsoup.parse(tableHtml);
            Element table = doc.select("table").first();
            Elements rows = table.select("tr");

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() == 3) { // Assuming each row has 3 cells representing Application, release, cve before remediation, cve after remediation
                    String application = cells.get(0).text();
                    String releaseDelivered = cells.get(1).text();
                    String cveCount = cells.get(2).text();
                    //String cveAfterRemediation = cells.get(3).text();

                    Remediation remediation = new Remediation(application, releaseDelivered, cveCount);
                    remediationDataList.add(remediation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return remediationDataList;
    }

    // Method to extract hyperlinks from the cell content
    private static String extractHyperlinks(Element cell) {
        //StringBuilder result = new StringBuilder();
        String result = "";
        Elements links = cell.select("a[href]");
        for (Element link : links) {
            //String linkText = link.text();
            String linkHref = link.attr("href");
            result = "https://confluence.vocalink.co.uk"+linkHref;
            break;
            //result.append("<a href='").append(linkHref).append("'>").append(linkText).append("</a>").append(", ");
        }
        return result;
    }

    /*private static void createSummaryWorksheet(String CSV_FILE_PATH, Map<String, String> applications) throws IOException {
        // Load the existing CSV file
        Workbook workbook;
        if (new File(CSV_FILE_PATH).exists()) {
            workbook = WorkbookFactory.create(new FileInputStream(CSV_FILE_PATH));
        } else {
            workbook = new XSSFWorkbook(); // Create a new workbook if it doesn't exist
        }
        // Create a new worksheet named 'Summary'
        Sheet summarySheet = workbook.createSheet("Summary");

        Cell cell = summarySheet.createRow(0).createCell(0);
        cell.setCellValue("Select Application");

        // Create a dropdown in cell A1 with the names of applications
        DataValidationHelper dvHelper = summarySheet.getDataValidationHelper();
        DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(applications.values().toArray(new String[0]));
        CellRangeAddressList addressList = new CellRangeAddressList(0, 0, 0, 0); // A1 cell
        DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
        summarySheet.addValidationData(validation);

        // Write the changes back to the CSV file
        FileOutputStream outputStream = new FileOutputStream(CSV_FILE_PATH);
        workbook.write(outputStream);

        // Close streams
        workbook.close();
        outputStream.close();
    }*/

    private static String getReportIdForGivenApplication(String applicationInternalId, String stage) throws IOException{
        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/reports/applications/" + applicationInternalId);

        String jsonResponse = connectToNexusIQ(urlObj);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        String reportId = null;

        for (JsonNode stageNode : rootNode) {
            if (stageNode.path("stage").asText().equalsIgnoreCase(stage)){
                String reportHtmlUrl = stageNode.path("reportHtmlUrl").asText();
                String[] parts = reportHtmlUrl.split("/");
                reportId = parts[parts.length-1];
            }
        }
        return reportId;
    }

    private static String getApplicationDependencyDataByApplicationPublicIdAndReportId(String publicId, String reportId) throws IOException {

        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/applications/" + publicId + "/reports/" + reportId + "/policy");

        String jsonResponse = connectToNexusIQ(urlObj);

        return jsonResponse;
    }

    private static List<ApplicationDependency> parseReportJsonToFormAListOfApplicationDependencies(String reportJson, List <Workflow> workflows) throws IOException {

        List<ApplicationDependency> applicationDependencies = new ArrayList<>();
        List<Component> components = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(reportJson);
        JsonNode dependenciesNode = rootNode.path("components");
        String applicationName = rootNode.path("application").path("name").asText();
        String applicationInternalId = rootNode.path("application").path("id").asText();

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

            List <PolicyViolation> allPolicyViolations = new LinkedList<>();
            PolicyViolation highestPolicyViolation = null;
            List<Component> otherAvailableVersions;

            if(groupId.isEmpty() || artifactId.isEmpty()){
                continue;
            }

            Coordinates coordinates = new Coordinates(format, groupId, artifactId, version, extension);
            /*Component component = new Component(dependencyPackageUrl, coordinates);
            components.add(component);*/

            /*if (!artifactId.equals("xstream")){
                continue;
            }*/
            //System.out.println(groupId+":"+artifactId);

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
                List <Constraint> constraintViolations = new ArrayList<>();
                JsonNode constViolations = policyViolationNode.path("constraints");

                for (JsonNode constViolation  : constViolations) {
                    String constraintId = constViolation.path("constraintId").asText();
                    String constraintName = constViolation.path("constraintName").asText();

                    JsonNode reasons = constViolation.path("conditions");

                    List <Reason> reasons1 = new ArrayList<>();
                    for (JsonNode reason  : reasons) {
                        String conditionSummary = reason.path("conditionSummary").asText();
                        String conditionReason = reason.path("conditionReason").asText();
                        Reason reason1 = new Reason(conditionSummary, conditionReason);
                        reasons1.add(reason1);
                    }
                    Constraint newConstraint = new Constraint(constraintId, constraintName);
                    newConstraint.setReasons(reasons1);
                    constraintViolations.add(newConstraint);
                }

                PolicyViolation pv = new PolicyViolation(policyId, policyName, policyThreatCategory, policyThreatLevel);
                pv.setConstraintViolations(constraintViolations);
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

            ApplicationDependency applicationDependency = new ApplicationDependency(applicationName, applicationInternalId, dependencyName, dependencyPackageUrl, isDirectDependency, isInnerSource, coordinates);
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

            for (Workflow workflow : workflows){
                if (workflow.getApplicationName().equalsIgnoreCase(applicationName) && workflow.getDependencyName().equalsIgnoreCase(dependencyName)){
                    applicationDependency.setWorkflow(workflow);
                    break;
                }
            }

            applicationDependencies.add(applicationDependency);
        }

        if(!applicationDependencies.isEmpty()){
            populateEachComponentWithPolicyViolationAndSecurityIssuesData(applicationDependencies, applicationInternalId);
        }

        return applicationDependencies;
    }

    private static void populateNextVersionWithNoPolicyViolation(ApplicationDependency applicationDependency){

        URL urlObj = null;
        try {
                urlObj = new URL(NEXUS_IQ_URL + "/api/v2/components/remediation/application/" + applicationDependency.getApplicationInternalId());

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
            } catch (IOException e){
                if (conn.getResponseCode() == 400){
                    System.out.println("Did not process component: "+ applicationDependency.getApplicationName() + " Coordinates: " + applicationDependency.getCoordinates().toString());
                } else {
                    e.printStackTrace();
                }
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
        } catch (IOException e) {
            System.out.println("Application: " + applicationDependency.getApplicationName() + " Coordinates: " + applicationDependency.getCoordinates().toString());
            e.printStackTrace();
        }
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
        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/evaluation/applications/" + applicationDependency.getApplicationInternalId() );
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
            Thread.sleep(3000);
            String jsonResponse = null;
            int attempts = 0;
            int maxAttempts = 15; // 30 seconds / 2 seconds per attempt

            while (jsonResponse == null && attempts < maxAttempts) {
                jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                if (jsonResponse != null) {
                    populatePolicyEvaluationResultData(components, jsonResponse, applicationDependency);
                    break; // Break the loop once data is populated
                } else {
                    attempts++;
                    try {
                        Thread.sleep(2000); // Wait for 2 seconds before trying again
                    } catch (InterruptedException e) {
                        // Handle the exception, if necessary
                        Thread.currentThread().interrupt();
                        System.out.println("Thread was interrupted. Exiting the check loop.");
                        return;
                    }
                }
            }

            if (jsonResponse == null) {
                System.out.println("Component: "+ components.get(0).getCoordinates().toString());
                System.out.println("Evaluation Results are not ready. jsonResponse is still null after 30 seconds. Moving on...");
            }

            /*if(checkIfEvaluatioResultIsReady(resultsUrl)){
                String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                populatePolicyEvaluationResultData(components, jsonResponse, applicationDependency);
            } else {
                System.out.println("Evaluation Result was not ready 1st attempt");
                Thread.sleep(1000);
                if(checkIfEvaluatioResultIsReady(resultsUrl)){
                    String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                    populatePolicyEvaluationResultData(components, jsonResponse, applicationDependency);
                } else {
                    System.out.println("Evaluation Result was not ready 2nd attempt");
                    populatePolicyEvaluationResultData(components, null, applicationDependency);
                }
            }*/
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
            System.out.println(components.get(0).getCoordinates());
            System.out.println("Error: populatePolicyEvaluationResultData JSON is null");
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultsNode = rootNode.path("results");

        if(!rootNode.has("results")){
            System.out.println("results were not ready line 680");
        }

        String policyId;
        String policyName;
        int policyThreatLevel = 1;
        int highestIdentifiedPolicyThreatLevel = 1;
        String policyThreatCategory;
        String threatCategory;

        String source;
        String reference;
        double severity;
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
                            List <Constraint> constraintViolations = new ArrayList<>();

                            JsonNode constViolations = policyViolations.path("constraintViolations");

                            for (JsonNode constViolation  : constViolations) {
                                String constraintId = constViolation.path("constraintId").asText();
                                String constraintName = constViolation.path("constraintName").asText();

                                JsonNode reasons = constViolation.path("reasons");

                                List <Reason> reasons1 = new ArrayList<>();
                                for (JsonNode reason  : reasons) {
                                    String reasonText = reason.path("reason").asText();
                                    Reason reason1 = new Reason(reasonText, "");
                                    reasons1.add(reason1);
                                }
                                Constraint newConstraint = new Constraint(constraintId, constraintName);
                                newConstraint.setReasons(reasons1);
                                constraintViolations.add(newConstraint);
                            }

                            PolicyViolation pv = new PolicyViolation(policyId, policyName, policyThreatCategory, policyThreatLevel);
                            pv.setConstraintViolations(constraintViolations);
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
                            severity = securityIssueNode.path("severity").asDouble();
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

    //private static void writeToExcel(List<ApplicationDependency> applicationDependencies, String fileName, String worksheetName) throws IOException {
    private static void writeToExcel(List<ApplicationDependency> applicationDependencies, Workbook workbook, String fileName) throws IOException {

        try {
            // Check if the workbook already exists
            /*if (new File(fileName).exists()) {
                workbook = WorkbookFactory.create(new FileInputStream(fileName));
            } else {
                workbook = new XSSFWorkbook(); // Create a new workbook if it doesn't exist
            }

            // Create a new worksheet with the specified name
            Sheet sheet = workbook.createSheet(worksheetName);

            // Write the common heading row
            Row headingRow = sheet.createRow(0);
            String[] columnNames = {"Application", "Component", "Dependency", "Threat level", "Policy Name", "Constraint Name",
                    "Constraint Reason", "Current Version", "Highest Available Version",
                    "Next Version With No Policy Violation",
                    "Next Version With No Policy Violation including Dependencies",
                    "More Versions With No Policy Violation", "CVE Count"};
            for (int i = 0; i < columnNames.length; i++) {
                Cell cell = headingRow.createCell(i);
                cell.setCellValue(columnNames[i]);
            }*/

            // Write data to the worksheet
            Sheet sheet = workbook.getSheet("Catalogue");
            int rowNumber = sheet.getLastRowNum()+1; // Start from row 1 after the heading row
            for (ApplicationDependency dependency : applicationDependencies) {

                int index = 0;
                for (PolicyViolation policyViolation : dependency.getAllPolicyViolations()){
                    for (Constraint constraint : policyViolation.getConstraintViolations()){
                        //for (Reason reason : constraint.getReasons()){
                        Row row = sheet.createRow(rowNumber++);

                        // Write data to cells in the row
                        Cell cell = row.createCell(0); // Application Name
                        cell.setCellValue(dependency.getApplicationName());

                        cell = row.createCell(1); // Dependency Name
                        cell.setCellValue(dependency.getDependencyName());

                        cell = row.createCell(2); // Dependency Package URL
                        cell.setCellValue(dependency.isDirectDependency() ? "Direct" : "Transitive");

                        cell = row.createCell(3); // Is Direct Dependency
                        cell.setCellValue(policyViolation.getPolicyThreatLevel());

                        cell = row.createCell(4); // Is Inner Source
                        cell.setCellValue(policyViolation.getPolicyName());

                        cell = row.createCell(5); // Is Inner Source
                        cell.setCellValue(constraint.getConstraintName());

                        String reasons = "";
                        for (Reason reason : constraint.getReasons()){
                            reasons += reason.getConditionReason();
                        }
                        cell = row.createCell(6); // Next Version With No Policy Violation
                        cell.setCellValue(reasons);

                        cell = row.createCell(7); // Next Version With No Policy Violation including Dependencies
                        cell.setCellValue(dependency.getCoordinates().getVersion());

                        cell = row.createCell(8); // More Versions With No Policy Violation
                        cell.setCellValue(dependency.getHighestAvailableVersion());

                        cell = row.createCell(9); // Next Version With No Security Issues
                        cell.setCellValue(dependency.getNextVersionWithNoPolicyViolation());

                        cell = row.createCell(10); // Highest Available Version
                        cell.setCellValue(dependency.getNextVersionWithNoPolicyViolationWithDependencies());

                        cell = row.createCell(11); // Highest Available Version
                        cell.setCellValue(dependency.getMoreVersionsWithNoPolicyViolation());

                        if (index == 0 && dependency.getAllSecurityIssues() != null){
                            cell = row.createCell(12);
                            cell.setCellValue(dependency.getAllSecurityIssues().size());
                        }

                        cell = row.createCell(13);
                        if (dependency.getWorkflow()!= null && !dependency.getWorkflow().getImpactedWorkflow().isEmpty()) {
                            cell.setCellValue(dependency.getWorkflow().getImpactedWorkflow());
                        }
                        cell = row.createCell(14);
                        if (dependency.getWorkflow()!= null && !dependency.getWorkflow().getTestCase().isEmpty()) {
                            cell.setCellValue(dependency.getWorkflow().getTestCase());
                        }

                        cell = row.createCell(15);
                        if (dependency.getWorkflow()!= null && !dependency.getWorkflow().getTestCaseLink().isEmpty()) {
                            cell.setCellValue(dependency.getWorkflow().getTestCaseLink());
                        }
                        //}
                    }
                    index ++;
                }

                /*StringBuilder cveCvssList = new StringBuilder();
                List<SecurityIssue> securityIssues;
                //Iterate through all versions of this dependencies
                for (Component comp : dependency.getOtherAvailableVersions()) {
                    //Pick the currently used version
                    if (comp.getCoordinates().equals(dependency.getCoordinates())) {
                        //Form a string of all 'CVE (CVSS)' for this dependency
                        securityIssues = comp.getAllSecurityIssues();
                        if (securityIssues != null && !securityIssues.isEmpty()) {
                            for (SecurityIssue cve : securityIssues) {
                                cveCvssList.append(cve.getReference())
                                        .append(" (CVSS:")
                                        .append(cve.getSeverity())
                                        .append(");  ");
                            }
                        }
                    }
                }*/


            }

            sheet.setColumnWidth(1, 18000);
            sheet.setColumnWidth(6, 20000);
            sheet.setColumnWidth(11, 9000);

            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            sheet.setDefaultColumnStyle(1, style);
            sheet.setDefaultColumnStyle(6, style);
            sheet.setDefaultColumnStyle(11, style);

            // Write the workbook to a file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeToExcelRemediationData(List<Remediation> remediations, Workbook workbook, String fileName) throws IOException {

        try {
            // Write data to the worksheet
            Sheet sheet = workbook.getSheet("RemediationData");
            int rowNumber = sheet.getLastRowNum()+1; // Start from row 1 after the heading row
            for (Remediation remediation : remediations) {

                Row row = sheet.createRow(rowNumber++);

                // Write data to cells in the row
                Cell cell = row.createCell(0); // Application Name
                cell.setCellValue(remediation.getApplicationName());

                cell = row.createCell(1); // Dependency Name
                cell.setCellValue(remediation.getReleaseDelivered());

                cell = row.createCell(2); // Dependency Package URL
                cell.setCellValue(remediation.getCveCount());
            }

            sheet.setColumnWidth(1, 18000);
            sheet.setColumnWidth(2, 18000);
            sheet.setColumnWidth(3, 18000);

            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            sheet.setDefaultColumnStyle(1, style);
            sheet.setDefaultColumnStyle(2, style);
            sheet.setDefaultColumnStyle(3, style);

            // Write the workbook to a file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*private static void writeToCSV(List<ApplicationDependency> applicationDependencies, String fileName) throws IOException {
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
                *//*int policyThreatLevelForLatestAvailableVersion = 0;
                if (detail.getNextVersionWithNoPolicyViolation() == null){
                    if(!detail.getOtherAvailableVersions().isEmpty() && detail.getOtherAvailableVersions().get(0).getHighestPolicyViolation() !=null){
                        policyThreatLevelForLatestAvailableVersion = detail.getOtherAvailableVersions().get(0).getHighestPolicyViolation().getPolicyThreatLevel();
                    }
                }*//*

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
                        *//*+ "," + (detail.getNextVersionWithNoPolicyViolation()==null?"Not Available (Latest available version has policy violation " +
                        "with Threat Level " + policyThreatLevelForLatestAvailableVersion:detail.getNextVersionWithNoPolicyViolation())*//*
                        + "," + detail.getMoreVersionsWithNoPolicyViolation());
            }
        }
    }*/

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

            // Get the HTTP response code
            int responseCode = connection.getResponseCode();

            // Check if the response code indicates success (200 OK)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } else {
                // Error response
                System.out.println("connectToNexusIQ : Returning null as there is an error response"+urlObj.toString());
                return null;
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

    private static void populateEachComponentWithPolicyViolationAndSecurityIssuesData(List<ApplicationDependency> dependencies, String appInternalId) throws IOException {
        URL urlObj = new URL(NEXUS_IQ_URL + "/api/v2/evaluation/applications/" + appInternalId );
        String userCredentials = IQ_USERNAME + ":" + IQ_PASSWORD;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

        // Construct the request body JSON string
        StringBuilder requestBodyBuilder = new StringBuilder();

        requestBodyBuilder.append("{\n");
        requestBodyBuilder.append("  \"components\": [\n");
        for (int i = 0; i < dependencies.size(); i++) {
            ApplicationDependency component = dependencies.get(i);
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
            if (i < dependencies.size() - 1) {
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
            Thread.sleep(3000);
            String jsonResponse = null;
            int attempts = 0;
            int maxAttempts = 15; // 30 seconds / 2 seconds per attempt

            while (jsonResponse == null && attempts < maxAttempts) {
                jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                if (jsonResponse != null) {
                    populateSecurityIssues(dependencies, jsonResponse);
                    break; // Break the looponce data is populated
                } else {
                    attempts++;
                    try {
                        Thread.sleep(2000); // Wait for 2 seconds before trying again
                    } catch (InterruptedException e) {
                        // Handle the exception, if necessary
                        Thread.currentThread().interrupt();
                        System.out.println("Thread was interrupted. Exiting the check loop.");
                        return;
                    }
                }
            }

            if (jsonResponse == null) {
                System.out.println("Inside populateEachComponentWithPolicyViolationAndSecurityIssuesData -  jsonResponse is still null after 30 seconds. Moving on...");
            }


            /*if(checkIfEvaluatioResultIsReady(resultsUrl)){
                String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                populateSecurityIssues(dependencies, jsonResponse);
            } else {
                Thread.sleep(1000);
                if(checkIfEvaluatioResultIsReady(resultsUrl)){
                    String jsonResponse = connectToNexusIQ(new URL(NEXUS_IQ_URL + "/" + resultsUrl));
                    populateSecurityIssues(dependencies, jsonResponse);
                } else {
                    return;
                }
            }*/
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void populateSecurityIssues(List<ApplicationDependency> components, String jsonResponse) throws IOException{
        if(jsonResponse == null){
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultsNode = rootNode.path("results");
        String threatCategory;
        String source;
        String reference;
        double severity;
        String status;
        String url;
        String cwe;

        StringBuilder stringBuilder = new StringBuilder();

        for (ApplicationDependency component : components) {
            for (JsonNode resultNode : resultsNode) {
                JsonNode componentIdentifier = resultNode.path("component").path("componentIdentifier");
                String format = componentIdentifier.path("format").asText();
                String groupId = componentIdentifier.path("coordinates").path("groupId").asText();
                String artifactId = componentIdentifier.path("coordinates").path("artifactId").asText();
                String version = componentIdentifier.path("coordinates").path("version").asText();
                String extension = componentIdentifier.path("coordinates").path("extension").asText();

                List <SecurityIssue> allSecurityIssues = new LinkedList<>();

                Coordinates responseCoordinates = new Coordinates(format, groupId, artifactId, version, extension);

                if(responseCoordinates.equals(component.getCoordinates())){
                    if(resultNode.has("securityData") && !resultNode.path("securityData").path("securityIssues").isEmpty()){
                        JsonNode securityIssues = resultNode.path("securityData").path("securityIssues");
                        for (JsonNode securityIssueNode : securityIssues) {
                            source = securityIssueNode.path("source").asText();
                            reference = securityIssueNode.path("reference").asText();
                            severity = securityIssueNode.path("severity").asDouble();
                            status = securityIssueNode.path("status").asText();
                            url = securityIssueNode.path("url").asText();
                            threatCategory = securityIssueNode.path("threatCategory").asText();
                            cwe = securityIssueNode.path("cwe").asText();

                            SecurityIssue securityIssue = new SecurityIssue(source, reference, severity, status, url, threatCategory, cwe);
                            allSecurityIssues.add(securityIssue);
                        }
                        component.setAllSecurityIssues(allSecurityIssues);
                    }
                }
            }
        }
    }
}
