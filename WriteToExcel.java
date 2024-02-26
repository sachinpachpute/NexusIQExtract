import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class WriteToExcel {
    private static int worksheetIndex = 1; // Keep track of the worksheet index

    private static void writeToExcel(List<ApplicationDependency> applicationDependencies, String fileName) throws IOException {
        Workbook workbook;
        try {
            // Check if the workbook already exists
            if (new File(fileName).exists()) {
                workbook = WorkbookFactory.create(new FileInputStream(fileName));
            } else {
                workbook = new XSSFWorkbook(); // Create a new workbook if it doesn't exist
            }

            // Create a new worksheet
            Sheet sheet = workbook.createSheet("Sheet" + worksheetIndex);

            // Write common heading row
            String[] headings = {"Application", "Component", "Dependency", "Threat level", "Policy Name",
                    "Associated CVEs", "Current Version", "Highest Available Version",
                    "Next Version With No Policy Violation", "Next Version With No Policy Violation including Dependencies",
                    "More Versions With No Policy Violation"};

            Row headingRow = sheet.createRow(0);
            for (int i = 0; i < headings.length; i++) {
                Cell cell = headingRow.createCell(i);
                cell.setCellValue(headings[i]);
            }

            // Write data to the worksheet
            int rowNumber = 1; // Start from the next row after the heading row
            for (ApplicationDependency dependency : applicationDependencies) {
                Row row = sheet.createRow(rowNumber++);
                // Write data to cells in the row
                // For example:
                // Cell cell = row.createCell(0);
                // cell.setCellValue(dependency.getName());
            }

            // Write the workbook to a file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }

            // Increment the worksheet index for the next call
            worksheetIndex++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
