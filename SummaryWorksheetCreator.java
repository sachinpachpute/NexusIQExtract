import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.Map;

public class SummaryWorksheetCreator {

    public void createSummaryWorksheet(String csvFilename, Map<String, String> applications) throws IOException {
        // Load the existing CSV file
        FileInputStream inputStream = new FileInputStream(csvFilename);
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

        // Create a new worksheet named 'Summary'
        workbook.createSheet("Summary");

        // Create a dropdown in cell A1 with the names of applications
        DataValidationHelper dvHelper = new XSSFWorkbook().getCreationHelper();
        String[] appNames = applications.values().toArray(new String[0]);
        String[] dropdownOptions = new String[appNames.length + 1];
        dropdownOptions[0] = "<Select Application>"; // Add default value
        System.arraycopy(appNames, 0, dropdownOptions, 1, appNames.length); // Copy application names
        String dropdownOptionsString = String.join(",", dropdownOptions);
        CellRangeAddressList addressList = new CellRangeAddressList(0, 0, 0, 0); // A1 cell
        DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(dropdownOptionsString);
        DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
        workbook.getSheet("Summary").addValidationData(validation);

        // Write the changes back to the CSV file
        FileOutputStream outputStream = new FileOutputStream(csvFilename);
        workbook.write(outputStream);

        // Close streams
        workbook.close();
        inputStream.close();
        outputStream.close();
    }

    public static void main(String[] args) {
        // Example usage
        String csvFilename = "example.csv"; // Provide the path to the CSV file
        Map<String, String> applications = Map.of(
                "1", "Application 1",
                "2", "Application 2",
                "3", "Application 3"
        ); // Example map of application IDs and names

        SummaryWorksheetCreator creator = new SummaryWorksheetCreator();
        try {
            creator.createSummaryWorksheet(csvFilename, applications);
            System.out.println("Summary worksheet created successfully.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
