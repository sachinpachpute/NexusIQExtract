import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class WriteToExcel {
    private static void writeToExcel(List<ApplicationDependency> applicationDependencies, String fileName, String worksheetName) throws IOException {
        Workbook workbook;
        try {
            // Check if the workbook already exists
            if (new File(fileName).exists()) {
                workbook = WorkbookFactory.create(new FileInputStream(fileName));
            } else {
                workbook = new XSSFWorkbook(); // Create a new workbook if it doesn't exist
            }

            // Create a new worksheet with the specified name
            Sheet sheet = workbook.createSheet(worksheetName);

            // Write data to the worksheet
            int rowNumber = 0;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
