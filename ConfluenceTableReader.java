import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;

public class ConfluenceTableReader {
    public static void main(String[] args) {
        String baseUrl = "https://confluence.vocalink.co.uk"; // Replace with your Confluence base URL
        String username = "sachin.pachpute"; // Replace with your Confluence username
        String password = "SachShrav@404"; // Replace with your Confluence password
        String pageId = "229413820"; // Replace with the actual page ID

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(baseUrl + "/rest/api/content/" + pageId + "?expand=body.view");
            request.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));

            HttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            JSONObject json = new JSONObject(responseBody);
            JSONObject view = json.getJSONObject("body").getJSONObject("view");
            String tableHtml = view.getString("value");

            Document doc = Jsoup.parse(tableHtml);
            Element table = doc.select("table").first();
            Elements rows = table.select("tr");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Table Data");

            int rowNum = 0;
            for (Element row : rows) {
                Row excelRow = sheet.createRow(rowNum++);
                Elements cells = row.select("td");
                int colNum = 0;
                for (Element cell : cells) {
                    Cell excelCell = excelRow.createCell(colNum++);
                    excelCell.setCellValue(cell.text());
                }
            }

            // Save the workbook to a file
            try (FileOutputStream fileOut = new FileOutputStream("TableData.xlsx")) {
                workbook.write(fileOut);
                System.out.println("Excel file created successfully!");
            }

            workbook.close();

            //System.out.println(responseBody);

            // Parse the JSON response to extract the table data
            // You'll need to implement this part based on your specific requirements
            // For example, use a JSON parser library or regex to extract the table content

            System.out.println("Table data from Confluence page:");
            // Print the extracted table data to the console
            // Modify this part to format and display the data as needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

