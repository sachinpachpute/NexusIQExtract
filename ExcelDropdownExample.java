import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelDropdownExample {
    public static void main(String[] args) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create a new Excel sheet
            Sheet sheet = workbook.createSheet("Sheet1");

            // Create a list of items for dropdown in A1
            String[] itemsA1 = {"Option 1", "Option 2", "Option 3"};

            // Create a list of items for dropdown in A2 corresponding to Option 1
            String[] itemsA2Option1 = {"A1-Option1-1", "A1-Option1-2", "A1-Option1-3"};
            // Create a list of items for dropdown in A2 corresponding to Option 2
            String[] itemsA2Option2 = {"A1-Option2-1", "A1-Option2-2", "A1-Option2-3"};
            // Create a list of items for dropdown in A2 corresponding to Option 3
            String[] itemsA2Option3 = {"A1-Option3-1", "A1-Option3-2", "A1-Option3-3"};

            // Create a cell for the dropdown in A1
            Row rowA1 = sheet.createRow(0);
            Cell cellA1 = rowA1.createCell(0);
            // Set the dropdown list of items for cell A1
            setDataValidation(workbook, sheet, itemsA1, 0, 0);

            // Create a cell for the dropdown in A2
            Row rowA2 = sheet.createRow(1);
            Cell cellA2 = rowA2.createCell(0);
            // Set initial dropdown list of items for cell A2 (corresponding to Option 1)
            setDataValidation(workbook, sheet, itemsA2Option1, 1, 0);

            // Set up data validation to dynamically change dropdown list in A2 based on value selected in A1
            DataValidationHelper validationHelper = sheet.getDataValidationHelper();
            DataValidationConstraint constraintA2 = validationHelper.createFormulaListConstraint("INDIRECT(\"A\" & MATCH(A1, A1:A3, 0) + 1 & \"_List\")");
            CellRangeAddressList addressListA2 = new CellRangeAddressList(1, 1, 0, 0);
            DataValidation dataValidationA2 = validationHelper.createValidation(constraintA2, addressListA2);
            sheet.addValidationData(dataValidationA2);

            // Add named ranges for the dropdown lists in A2
            for (int i = 0; i < itemsA1.length; i++) {
                String namedRangeName = "A" + (i + 1) + "_List";
                createNamedRange(sheet, namedRangeName, i == 0 ? itemsA2Option1 : i == 1 ? itemsA2Option2 : itemsA2Option3);
            }

            // Write the Excel workbook to a file
            try (FileOutputStream fileOut = new FileOutputStream("workbook.xlsx")) {
                workbook.write(fileOut);
            }

            System.out.println("Excel file with dropdowns created successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to set data validation for a cell
    private static void setDataValidation(Workbook workbook, Sheet sheet, String[] items, int rowNum, int colNum) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(items);
        CellRangeAddressList addressList = new CellRangeAddressList(rowNum, rowNum, colNum, colNum);
        DataValidation dataValidation = validationHelper.createValidation(constraint, addressList);
        sheet.addValidationData(dataValidation);
    }

    // Method to create named range for dropdown list
    private static void createNamedRange(Sheet sheet, String namedRangeName, String[] items) {
        Name namedRange = sheet.getWorkbook().createName();
        namedRange.setNameName(namedRangeName);
        namedRange.setRefersToFormula(sheet.getSheetName() + "!$" + (char) ('A' + namedRangeName.charAt(1) - '0') + "$2:$" + (char) ('A' + namedRangeName.charAt(1) - '0') + "$" + (items.length + 1));
    }
}
