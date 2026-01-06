package expenseApproval;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import com.revature.repository.Approval;
import com.revature.service.ExpenseService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ExpenseService.generateCsvReport() method
 */
@Epic("Expense Management")
@Feature("CSV Report Generation")
@ExtendWith(MockitoExtension.class)
class ExpenseServiceCsvReportTest
{

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    private ExpenseService expenseService;

    @BeforeEach
    @Step("Set up test dependencies")
    void setUp()
    {
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
    }

    /**
     * Test Case 1: Verify CSV header row format
     */
    @Test
    @DisplayName("Should generate correct CSV header row format")
    @Story("CSV Header Validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that the CSV report generates the correct header row with all required columns in the proper format")
    void testGenerateCsvReport_HeaderRowFormat()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                "Office supplies", "2024-01-15", "approved", 201,
                "Looks good", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("Generated CSV Report", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        assertTrue(lines.length >= 1, "CSV should have at least header row");

        String header = lines[0];
        Allure.step("Verify header format matches expected", () -> {
            assertEquals("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date",
                    header, "Header format should match expected format");
        });

        // Verify all expected columns are present
        String[] columns = header.split(",");
        Allure.step("Verify header has 9 columns", () -> {
            assertEquals(9, columns.length, "Header should have 9 columns");
        });

        assertEquals("Expense ID", columns[0]);
        assertEquals("Employee", columns[1]);
        assertEquals("Amount", columns[2]);
        assertEquals("Description", columns[3]);
        assertEquals("Date", columns[4]);
        assertEquals("Status", columns[5]);
        assertEquals("Reviewer", columns[6]);
        assertEquals("Comment", columns[7]);
        assertEquals("Review Date", columns[8]);
    }

    /**
     * Test Case 2: Verify CSV data rows format
     */
    @Test
    @DisplayName("Should generate correct CSV data rows format")
    @Story("CSV Data Row Formatting")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that CSV data rows are formatted correctly, including proper handling of null values")
    void testGenerateCsvReport_DataRowsFormat()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.50,
                "Office supplies", "2024-01-15", "approved", 201,
                "Approved", "2024-01-16"));
        expenses.add(createExpenseWithUser(2, 102, "Jane Smith", 250.75,
                "Travel expenses", "2024-01-20", "pending", null,
                null, null));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Multiple Rows", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        Allure.step("Verify CSV has header + 2 data rows", () -> {
            assertEquals(3, lines.length, "CSV should have header + 2 data rows");
        });

        // Verify first data row
        String dataRow1 = lines[1];
        Allure.step("Verify first data row contains expected values", () -> {
            assertTrue(dataRow1.contains("1,"), "Should contain expense ID 1");
            assertTrue(dataRow1.contains("John Doe"), "Should contain employee name");
            assertTrue(dataRow1.contains("100.5"), "Should contain amount");
            assertTrue(dataRow1.contains("Office supplies"), "Should contain description");
            assertTrue(dataRow1.contains("2024-01-15"), "Should contain date");
            assertTrue(dataRow1.contains("approved"), "Should contain status");
            assertTrue(dataRow1.contains("201"), "Should contain reviewer ID");
            assertTrue(dataRow1.contains("Approved"), "Should contain comment");
            assertTrue(dataRow1.contains("2024-01-16"), "Should contain review date");
        });

        // Verify second data row with null values
        String dataRow2 = lines[2];
        Allure.step("Verify second data row with null values", () -> {
            assertTrue(dataRow2.contains("2,"), "Should contain expense ID 2");
            assertTrue(dataRow2.contains("Jane Smith"), "Should contain employee name");
            assertTrue(dataRow2.contains("250.75"), "Should contain amount");
            assertTrue(dataRow2.contains("pending"), "Should contain status");
        });

        // Count commas to ensure proper formatting with null values
        long commaCount = dataRow2.chars().filter(ch -> ch == ',').count();
        assertEquals(8, commaCount, "Should have 8 commas for 9 columns");
    }

    /**
     * Test Case 3: Verify comma escaping in values
     */
    @Test
    @DisplayName("Should properly escape commas in CSV values")
    @Story("CSV Special Character Handling")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that commas within CSV field values are properly escaped using quotes")
    @Issue("CSV-001")
    void testGenerateCsvReport_CommaEscaping()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "Doe, John", 100.0,
                "Office supplies, paper, pens", "2024-01-15", "approved", 201,
                "Approved, looks good", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Comma Escaping", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        String dataRow = lines[1];

        // Values with commas should be quoted
        Allure.step("Verify comma escaping in employee name", () -> {
            assertTrue(dataRow.contains("\"Doe, John\""),
                    "Employee name with comma should be quoted");
        });

        Allure.step("Verify comma escaping in description", () -> {
            assertTrue(dataRow.contains("\"Office supplies, paper, pens\""),
                    "Description with commas should be quoted");
        });

        Allure.step("Verify comma escaping in comment", () -> {
            assertTrue(dataRow.contains("\"Approved, looks good\""),
                    "Comment with comma should be quoted");
        });

        // Verify the structure is maintained despite commas in values
        assertFalse(dataRow.matches(".*,{2,}.*"),
                "Should not have consecutive commas");
    }

    /**
     * Test Case 4: Verify quote escaping in values
     */
    @Test
    @DisplayName("Should properly escape quotes in CSV values")
    @Story("CSV Special Character Handling")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that double quotes within CSV field values are properly escaped as double-double quotes")
    @Issue("CSV-002")
    void testGenerateCsvReport_QuoteEscaping()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John \"Johnny\" Doe", 100.0,
                "Client said \"urgent\"", "2024-01-15", "approved", 201,
                "Manager noted \"exceptional\"", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Quote Escaping", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        String dataRow = lines[1];

        // Quotes should be escaped as double quotes ""
        Allure.step("Verify quote escaping in employee name", () -> {
            assertTrue(dataRow.contains("\"John \"\"Johnny\"\" Doe\""),
                    "Employee name with quotes should have escaped quotes");
        });

        Allure.step("Verify quote escaping in description", () -> {
            assertTrue(dataRow.contains("\"Client said \"\"urgent\"\"\""),
                    "Description with quotes should have escaped quotes");
        });

        Allure.step("Verify quote escaping in comment", () -> {
            assertTrue(dataRow.contains("\"Manager noted \"\"exceptional\"\"\""),
                    "Comment with quotes should have escaped quotes");
        });
    }

    /**
     * Test Case 5: Verify newline handling in values
     */
    @Test
    @DisplayName("Should properly handle newlines in CSV values")
    @Story("CSV Special Character Handling")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that newline characters within CSV field values are properly handled by quoting the field")
    void testGenerateCsvReport_NewlineHandling()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                "Line 1\nLine 2\nLine 3", "2024-01-15", "approved", 201,
                "Comment line 1\nComment line 2", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Newlines", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);

        // Values with newlines should be quoted
        Allure.step("Verify newline handling in description", () -> {
            assertTrue(csvReport.contains("\"Line 1\nLine 2\nLine 3\""),
                    "Description with newlines should be quoted");
        });

        Allure.step("Verify newline handling in comment", () -> {
            assertTrue(csvReport.contains("\"Comment line 1\nComment line 2\""),
                    "Comment with newlines should be quoted");
        });

        // Split by actual row boundaries (not internal newlines)
        // The CSV should have 2 logical rows: header + 1 data row
        // But the string might have more \n due to values containing newlines
        String[] allLines = csvReport.split("\n");
        assertTrue(allLines.length > 2,
                "Should have more than 2 lines due to newlines in values");
    }

    /**
     * Test Case 6: Verify empty list produces header only
     */
    @Test
    @DisplayName("Should generate header only when expense list is empty")
    @Story("CSV Edge Cases")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that an empty expense list generates a CSV with only the header row")
    void testGenerateCsvReport_EmptyListHeaderOnly()
    {
        // Arrange
        List<ExpenseWithUser> emptyExpenses = new ArrayList<>();

        // Act
        String csvReport = expenseService.generateCsvReport(emptyExpenses);
        Allure.addAttachment("Empty CSV Report", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        Allure.step("Verify only header row exists", () -> {
            assertEquals(1, lines.length, "CSV should have only header row");
        });

        String header = lines[0];
        assertEquals("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date",
                header, "Header should be present even with empty list");

        // Verify no data rows
        assertFalse(csvReport.contains(",101,"), "Should not contain any data");
    }

    /**
     * Test Case 7: Verify special characters handled correctly
     */
    @Test
    @DisplayName("Should properly handle special characters in CSV values")
    @Story("CSV Special Character Handling")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that various special characters (unicode, symbols, accents) are properly preserved in CSV output")
    void testGenerateCsvReport_SpecialCharactersHandling()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();

        // Test various special characters
        expenses.add(createExpenseWithUser(1, 101, "José García", 100.0,
                "Café & Restaurant", "2024-01-15", "approved", 201,
                "€500 budget; 50% discount", "2024-01-16"));
        expenses.add(createExpenseWithUser(2, 102, "François Müller", 200.0,
                "Tech supplies @ store #5", "2024-01-17", "denied", 202,
                "Over budget! $$$", "2024-01-18"));
        expenses.add(createExpenseWithUser(3, 103, "李明", 150.0,
                "Conference (APAC)", "2024-01-20", "pending", null,
                null, null));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Special Characters", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        assertEquals(4, lines.length, "CSV should have header + 3 data rows");

        // Verify special characters are preserved
        Allure.step("Verify accented characters preserved", () -> {
            assertTrue(csvReport.contains("José García"), "Should preserve accented characters");
            assertTrue(csvReport.contains("Café & Restaurant"), "Should preserve ampersand and accents");
            assertTrue(csvReport.contains("François Müller"), "Should preserve umlaut");
        });

        Allure.step("Verify special symbols preserved", () -> {
            assertTrue(csvReport.contains("€500 budget; 50% discount"), "Should preserve euro sign and semicolon");
            assertTrue(csvReport.contains("Tech supplies @ store #5"), "Should preserve @ and # symbols");
            assertTrue(csvReport.contains("Over budget! $$$"), "Should preserve exclamation and dollar signs");
        });

        Allure.step("Verify Unicode characters preserved", () -> {
            assertTrue(csvReport.contains("李明"), "Should preserve Unicode characters");
            assertTrue(csvReport.contains("Conference (APAC)"), "Should preserve parentheses");
        });

        // Verify values with special characters requiring quotes are quoted
        String dataRow2 = lines[2];
        assertTrue(dataRow2.contains("€500 budget; 50% discount"),
                "Comment without CSV special chars should not be quoted");
    }

    /**
     * Test Case 8: Verify null values in data rows
     */
    @Test
    @DisplayName("Should handle null values correctly in CSV")
    @Story("CSV Edge Cases")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that null values in expense data are handled correctly, maintaining proper CSV structure")
    void testGenerateCsvReport_NullValuesHandling() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();

        // Expense with all null optional fields
        expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                "Office supplies", "2024-01-15", "pending", null,
                null, null));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Null Values", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        String dataRow = lines[1];

        // Split the data row by comma (being careful about quoted values)
        String[] fields = dataRow.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        // Verify structure with null values
        assertTrue(dataRow.contains("pending"), "Should contain status");

        // Count commas to ensure proper CSV structure
        long commaCount = dataRow.chars().filter(ch -> ch == ',').count();
        Allure.step("Verify proper CSV structure with null values", () -> {
            assertEquals(8, commaCount, "Should have 8 commas for 9 columns even with null values");
        });

        // Verify the row ends correctly (no extra content after last comma)
        assertTrue(dataRow.matches(".*,\\s*$") || dataRow.endsWith(","),
                "Row should end with comma when last field is null");
    }

    /**
     * Test Case 9: Verify multiple rows with mixed scenarios
     */
    @Test
    @DisplayName("Should handle multiple rows with various data scenarios")
    @Story("CSV Complex Scenarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that CSV generation handles multiple rows with different edge cases simultaneously")
    void testGenerateCsvReport_MultipleRowsMixedScenarios() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();

        // Mix of different scenarios
        expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                "Simple description", "2024-01-15", "approved", 201,
                "Good", "2024-01-16"));
        expenses.add(createExpenseWithUser(2, 102, "Jane, Smith", 200.0,
                "Description with \"quotes\" and, commas", "2024-01-17", "pending", null,
                null, null));
        expenses.add(createExpenseWithUser(3, 103, "Bob\nJohnson", 150.0,
                "Normal text", "2024-01-20", "denied", 203,
                "Multiline\ncomment\nhere", "2024-01-21"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("CSV with Mixed Scenarios", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);

        // Should contain all three expense IDs
        Allure.step("Verify all expense IDs present", () -> {
            assertTrue(csvReport.contains(",1,") || csvReport.contains("1,"),
                    "Should contain expense ID 1");
            assertTrue(csvReport.contains(",2,") || csvReport.contains("2,"),
                    "Should contain expense ID 2");
            assertTrue(csvReport.contains(",3,") || csvReport.contains("3,"),
                    "Should contain expense ID 3");
        });

        // Verify proper escaping
        Allure.step("Verify proper escaping of special characters", () -> {
            assertTrue(csvReport.contains("\"Jane, Smith\""), "Comma in name should be quoted");
            assertTrue(csvReport.contains("\"Description with \"\"quotes\"\" and, commas\""),
                    "Mixed special chars should be properly escaped");
            assertTrue(csvReport.contains("\"Bob\nJohnson\""), "Newline in name should be quoted");
            assertTrue(csvReport.contains("\"Multiline\ncomment\nhere\""),
                    "Multiline comment should be quoted");
        });
    }

    /**
     * Test Case 10: Verify complete CSV structure
     */
    @Test
    @DisplayName("Should generate valid CSV structure with all components")
    @Story("CSV Structure Validation")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Comprehensive validation of complete CSV structure including header, data rows, and formatting")
    void testGenerateCsvReport_CompleteStructure() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "Employee One", 500.00,
                "Travel to conference", "2024-01-10", "approved", 201,
                "Approved for Q1 travel", "2024-01-11"));
        expenses.add(createExpenseWithUser(2, 102, "Employee Two", 75.50,
                "Office lunch", "2024-01-15", "approved", 201,
                "Team building event", "2024-01-16"));
        expenses.add(createExpenseWithUser(3, 103, "Employee Three", 1250.00,
                "New laptop", "2024-01-20", "pending", null,
                null, null));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);
        Allure.addAttachment("Complete CSV Report", "text/csv", csvReport);

        // Assert
        assertNotNull(csvReport);
        assertFalse(csvReport.isEmpty(), "CSV should not be empty");

        String[] lines = csvReport.split("\n");
        Allure.step("Verify CSV has correct number of rows", () -> {
            assertEquals(4, lines.length, "Should have 1 header + 3 data rows");
        });

        // Verify each line has correct number of fields
        Allure.step("Verify each line has correct field count", () -> {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                long commaCount = line.chars().filter(ch -> ch == ',').count();
                assertEquals(8, commaCount,
                        "Line " + i + " should have 8 commas (9 fields)");
            }
        });

        // Verify CSV ends with newline
        assertTrue(csvReport.endsWith("\n"), "CSV should end with newline");
    }

    // Helper method to create ExpenseWithUser objects for testing
    @Step("Create test expense with user: ID={expenseId}, User={username}, Amount={amount}")
    private ExpenseWithUser createExpenseWithUser(int expenseId, int userId, String username,
                                                  double amount, String description, String date,
                                                  String status, Integer reviewerId,
                                                  String comment, String reviewDate) {
        Expense expense = new Expense(expenseId, userId, amount, description, date);

        User user = new User();
        user.setId(userId);
        user.setUsername(username);

        Approval approval = new Approval();
        approval.setExpenseId(expenseId);
        approval.setStatus(status);
        approval.setReviewer(reviewerId);
        approval.setComment(comment);
        approval.setReviewDate(reviewDate);

        return new ExpenseWithUser(expense, user, approval);
    }
}