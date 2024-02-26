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

            // Write the common heading row
            Row headingRow = sheet.createRow(0);
            String[] columnNames = {"Application", "Component", "Dependency", "Threat level", "Policy Name",
                    "Associated CVEs", "Current Version", "Highest Available Version",
                    "Next Version With No Policy Violation",
                    "Next Version With No Policy Violation including Dependencies",
                    "More Versions With No Policy Violation"};
            for (int i = 0; i < columnNames.length; i++) {
                Cell cell = headingRow.createCell(i);
                cell.setCellValue(columnNames[i]);
            }

            // Write data to the worksheet
            int rowNumber = 1; // Start from row 1 after the heading row
            for (ApplicationDependency dependency : applicationDependencies) {
                Row row = sheet.createRow(rowNumber++);
                // Write data to cells in the row
                Cell cell = row.createCell(0); // Application Name
                cell.setCellValue(dependency.getApplicationName());

                cell = row.createCell(1); // Dependency Name
                cell.setCellValue(dependency.getDependencyName());

                cell = row.createCell(2); // Dependency Package URL
                cell.setCellValue(dependency.getDependencyPackageUrl());

                cell = row.createCell(3); // Is Direct Dependency
                cell.setCellValue(dependency.isDirectDependency());

                cell = row.createCell(4); // Is Inner Source
                cell.setCellValue(dependency.isInnerSource());

                cell = row.createCell(5); // Next Version With No Policy Violation
                cell.setCellValue(dependency.getNextVersionWithNoPolicyViolation());

                cell = row.createCell(6); // Next Version With No Policy Violation including Dependencies
                cell.setCellValue(dependency.getNextVersionWithNoPolicyViolationWithDependencies());

                cell = row.createCell(7); // More Versions With No Policy Violation
                cell.setCellValue(dependency.getMoreVersionsWithNoPolicyViolation());

                cell = row.createCell(8); // Next Version With No Security Issues
                cell.setCellValue(dependency.getNextVersionWithNoSecurityIssues());

                cell = row.createCell(9); // Highest Available Version
                cell.setCellValue(dependency.getHighestAvailableVersion());
            }

            // Write the workbook to a file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
}
