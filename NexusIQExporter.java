import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Scanner;

public class NexusIQExporter {
    private static final String IQ_URL = "https://nexus-iq.vocalink.co.uk";
    private static final String USERNAME = "tanisha.sethi";
    private static final String PASSWORD = "V0calink2023!";
    private static final String APPLICATION_ID = "78a795c3d05044c4ba7297c15bdc7c0b";

    public static void main(String[] args) {
        try {
            URL url = new URL(IQ_URL + "/api/v2/components/" + APPLICATION_ID);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            String userCredentials = USERNAME + ":" + PASSWORD;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
            conn.setRequestProperty("Authorization", basicAuth);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                Scanner scanner = new Scanner(url.openStream());
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                // Parse JSON response
                // Assuming JSON structure is like { "components": [ { "name": "library1", ... }, { "name": "library2", ... }, ... ] }
                String jsonResponse = response.toString();
                String[] libraries = jsonResponse.split("\"name\":\"");
                PrintWriter writer = new PrintWriter(new FileWriter("libraries.txt"));
                for (int i = 1; i < libraries.length; i++) {
                    String library = libraries[i].split("\"")[0];
                    writer.println(library);
                }
                writer.close();
                System.out.println("Library list exported to libraries.txt");
            } else {
                System.out.println("Failed to fetch library list. Response code: " + responseCode);
            }
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
