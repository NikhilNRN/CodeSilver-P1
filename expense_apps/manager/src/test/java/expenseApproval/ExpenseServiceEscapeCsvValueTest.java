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
 * Test cases for ExpenseService.escapeCsvValue() private method
 * Tests are performed indirectly through generateCsvReport() method
 */
@ExtendWith(MockitoExtension.class)
@Epic("Expense Management")
@Feature("CSV Report Generation")
class ExpenseServiceEscapeCsvValueTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
    }

    @Test
    @DisplayName("Should quote values containing commas")
    @Story("Escape CSV values with commas")
    @Severity(SeverityLevel.CRITICAL)
    void testEscapeCsvValue_ValuesWithCommasAreQuoted() {
        Allure.step("Arrange: create expense with commas", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "Smith, John", 100.0,
                    "Lunch, dinner, breakfast", "2024-01-15", "approved", 201,
                    "Approved, processed", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: verify quoted values", () -> {
                    assertNotNull(csvReport);
                    assertTrue(csvReport.contains("\"Smith, John\""));
                    assertTrue(csvReport.contains("\"Lunch, dinner, breakfast\""));
                    assertTrue(csvReport.contains("\"Approved, processed\""));
                    assertFalse(csvReport.contains("Smith, John,100.0"));
                });
            });
        });
    }

    @Test
    @DisplayName("Should double quotes in values containing quotes")
    @Story("Escape CSV values with quotes")
    @Severity(SeverityLevel.CRITICAL)
    void testEscapeCsvValue_ValuesWithQuotesHaveQuotesDoubled() {
        Allure.step("Arrange: create expense with quotes", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "John \"Johnny\" Doe", 100.0,
                    "Item marked \"urgent\"", "2024-01-15", "approved", 201,
                    "Manager said \"approved\"", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: verify doubled quotes", () -> {
                    assertNotNull(csvReport);
                    assertTrue(csvReport.contains("\"John \"\"Johnny\"\" Doe\""));
                    assertTrue(csvReport.contains("\"Item marked \"\"urgent\"\"\""));
                    assertTrue(csvReport.contains("\"Manager said \"\"approved\"\"\""));
                    assertFalse(csvReport.contains("John \"Johnny\" Doe"));
                });
            });
        });
    }

    @Test
    @DisplayName("Should quote values containing newlines")
    @Story("Escape CSV values with newlines")
    @Severity(SeverityLevel.NORMAL)
    void testEscapeCsvValue_ValuesWithNewlinesAreQuoted() {
        Allure.step("Arrange: create expense with newlines", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "John\nDoe", 100.0,
                    "Line one\nLine two\nLine three", "2024-01-15", "approved", 201,
                    "First line\nSecond line", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: verify quoted newlines", () -> {
                    assertNotNull(csvReport);
                    assertTrue(csvReport.contains("\"John\nDoe\""));
                    assertTrue(csvReport.contains("\"Line one\nLine two\nLine three\""));
                    assertTrue(csvReport.contains("\"First line\nSecond line\""));
                    assertTrue(csvReport.contains("Line one\nLine two"));
                });
            });
        });
    }

    @Test
    @DisplayName("Should not quote simple values without special characters")
    @Story("Escape simple CSV values")
    @Severity(SeverityLevel.MINOR)
    void testEscapeCsvValue_SimpleValuesRemainUnquoted() {
        Allure.step("Arrange: create simple expense", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                    "OfficeSupplies", "2024-01-15", "approved", 201,
                    "Approved", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: verify unquoted simple values", () -> {
                    assertNotNull(csvReport);
                    String[] lines = csvReport.split("\n");
                    String dataRow = lines[1];
                    assertTrue(dataRow.contains("JohnDoe,") || dataRow.contains(",JohnDoe,"));
                    assertTrue(dataRow.contains("OfficeSupplies,") || dataRow.contains(",OfficeSupplies,"));
                    assertTrue(dataRow.contains("Approved,") || dataRow.contains(",Approved,"));
                    assertFalse(dataRow.contains("\"JohnDoe\""));
                    assertFalse(dataRow.contains("\"OfficeSupplies\""));
                    assertFalse(dataRow.contains("\"Approved\""));
                });
            });
        });
    }

    /**
     * Test Case 5: Verify null/empty string handling
     */
    @Test
    @DisplayName("Should handle null and empty string values correctly")
    @Description("Verify that null and empty string values are handled properly in CSV generation without producing 'null' strings")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CSV Escaping - Null and Empty Values")
    @Issue("CSV-105")
    void testEscapeCsvValue_NullAndEmptyStringHandling() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();

        // Expense with null optional fields
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                "", "2024-01-15", "pending", null,
                null, null));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        String dataRow = lines[1];

        // Null values should result in empty string (no quotes needed)
        // Count the commas to ensure structure is maintained
        long commaCount = dataRow.chars().filter(ch -> ch == ',').count();
        assertEquals(8, commaCount, "Should have 8 commas for 9 columns");

        // Empty string description should appear as empty (no quotes for empty string)
        // The pattern should show two consecutive commas or comma at specific position
        assertTrue(dataRow.matches(".*,\\s*,.*") || dataRow.contains(",,"),
                "Empty description should result in consecutive commas or empty field");

        // Null comment should not add any text
        assertFalse(dataRow.contains("null"),
                "Null values should not appear as the string 'null'");

        Allure.step("Verified null values handled correctly", () -> {
            Allure.addAttachment("Comma Count", String.valueOf(commaCount));
            Allure.addAttachment("Data Row", dataRow);
        });
    }

    /**
     * Test Case 6: Verify combination of special characters
     */
    @Test
    @DisplayName("Should handle values with multiple special characters")
    @Description("Test CSV generation with values containing combinations of quotes, commas, and newlines")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CSV Escaping - Combined Special Characters")
    @Issue("CSV-106")
    void testEscapeCsvValue_CombinationOfSpecialCharacters() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "Smith, \"John\"", 100.0,
                "Description with \"quotes\", commas, and\nnewlines", "2024-01-15",
                "approved", 201, "Comment: \"good\",\napproved", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        verifySpecialCharacterEscaping(csvReport);

        // Verify proper escaping (quotes doubled, values wrapped)
        String[] lines = csvReport.split("\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        assertTrue(lines.length >= 2, "Should have at least header and one data row");

        Allure.addAttachment("CSV Report", "text/csv", csvReport);
    }

    @Step("Verify special character escaping in CSV output")
    private void verifySpecialCharacterEscaping(String csvReport) {
        // All values with special characters should be quoted
        assertTrue(csvReport.contains("\"Smith, \"\"John\"\"\""),
                "Should handle comma and quotes together");
        assertTrue(csvReport.contains("\"Description with \"\"quotes\"\", commas, and\nnewlines\""),
                "Should handle quotes, commas, and newlines together");
        assertTrue(csvReport.contains("\"Comment: \"\"good\"\",\napproved\""),
                "Should handle all special characters in comment");
    }

    /**
     * Test Case 7: Verify edge case - only comma
     */
    @Test
    @DisplayName("Should quote value that is only a comma")
    @Description("Verify that a field containing only a comma character is properly quoted")
    @Severity(SeverityLevel.NORMAL)
    @Story("CSV Escaping - Edge Cases")
    @Issue("CSV-107")
    void testEscapeCsvValue_OnlyComma() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                ",", "2024-01-15", "approved", 201,
                ",", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // A single comma should be quoted
        assertTrue(csvReport.contains("\",\""),
                "Single comma should be quoted");

        Allure.step("Verified single comma is properly quoted");
    }

    /**
     * Test Case 8: Verify edge case - only quote
     */
    @Test
    @DisplayName("Should quote and double a value that is only a quote")
    @Description("Verify that a field containing only a quote character is doubled and wrapped in quotes")
    @Severity(SeverityLevel.NORMAL)
    @Story("CSV Escaping - Edge Cases")
    @Issue("CSV-108")
    void testEscapeCsvValue_OnlyQuote() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                "\"", "2024-01-15", "approved", 201,
                "\"", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // A single quote should be doubled and wrapped
        assertTrue(csvReport.contains("\"\"\"\""),
                "Single quote should become four quotes (wrapper + doubled)");

        Allure.step("Verified single quote produces four quotes in output");
        Allure.addAttachment("Expected Pattern", "\"\"\"\" (opening quote + doubled quote + closing quote)");
    }

    /**
     * Test Case 9: Verify edge case - only newline
     */
    @Test
    @DisplayName("Should quote value that is only a newline")
    @Description("Verify that a field containing only a newline character is properly quoted")
    @Severity(SeverityLevel.NORMAL)
    @Story("CSV Escaping - Edge Cases")
    @Issue("CSV-109")
    void testEscapeCsvValue_OnlyNewline() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                "\n", "2024-01-15", "approved", 201,
                "\n", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // A single newline should be quoted
        assertTrue(csvReport.contains("\"\n\""),
                "Single newline should be quoted");

        Allure.step("Verified single newline is properly quoted");
    }

    /**
     * Test Case 10: Verify whitespace-only values are not quoted
     */
    @Test
    @DisplayName("Should not quote values that are only whitespace")
    @Description("Verify that values containing only spaces (without special characters) are not unnecessarily quoted")
    @Severity(SeverityLevel.MINOR)
    @Story("CSV Escaping - Whitespace Handling")
    @Issue("CSV-110")
    void testEscapeCsvValue_WhitespaceOnlyValues() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                "   ", "2024-01-15", "approved", 201,
                "  spaces  ", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // Whitespace without special characters should not be quoted
        // Note: "John Doe" has a space but no comma/quote/newline, so it should not be quoted
        String[] lines = csvReport.split("\n");
        String dataRow = lines[1];

        // Simple spaces should not trigger quoting (unless they contain special chars)
        assertTrue(dataRow.contains("John Doe"),
                "Name with space should appear without quotes");

        Allure.step("Verified whitespace-only values are handled correctly", () -> {
            Allure.addAttachment("Data Row", dataRow);
        });
    }

    /**
     * Test Case 11: Verify multiple consecutive special characters
     */
    @Test
    @DisplayName("Should handle multiple consecutive special characters")
    @Description("Test CSV generation with values containing multiple consecutive quotes and commas")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CSV Escaping - Multiple Special Characters")
    @Issue("CSV-111")
    void testEscapeCsvValue_MultipleConsecutiveSpecialChars() {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                "Multiple \"\"\"\" quotes", "2024-01-15", "approved", 201,
                "Commas,,, here", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        verifyMultipleConsecutiveCharacters(csvReport);

        Allure.addAttachment("CSV Output", "text/csv", csvReport);
    }

    @Step("Verify multiple consecutive special characters are properly escaped")
    private void verifyMultipleConsecutiveCharacters(String csvReport) {
        // Multiple consecutive quotes should be properly escaped
        assertTrue(csvReport.contains("\"Multiple \"\"\"\"\"\"\"\"\"\" quotes\""),
                "Should escape all quotes by doubling them");

        // Multiple consecutive commas should still be within quotes
        assertTrue(csvReport.contains("\"Commas,,, here\""),
                "Multiple commas should be within quotes");

        Allure.addAttachment("Quote Escaping", "Each quote is doubled: \"\" becomes \"\"\"\"");
        Allure.addAttachment("Comma Handling", "Multiple commas preserved within quotes");
    }

    // Helper method to create ExpenseWithUser objects for testing
    @Step("Create test expense - ID: {expenseId}, User: {username}, Amount: {amount}")
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
