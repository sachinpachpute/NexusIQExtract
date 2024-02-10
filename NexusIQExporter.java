import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class NexusIQExporter {
    private static final String IQ_URL = "https://nexus-iq.vocalink.co.uk";
    private static final String USERNAME = "tanisha.sethi";
    private static final String PASSWORD = "V0calink2023!";
    private static final String APPLICATION_ID = "78a795c3d05044c4ba7297c15bdc7c0b";
    private static final String CSV_FILE_PATH = "organization_list.csv";

    public static void main(String[] args) {
        try {
            URL url = new URL(IQ_URL + "/api/v2/organizations");
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

                System.out.println("List of Organizations:");
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
}
