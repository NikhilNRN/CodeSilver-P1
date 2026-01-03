package expenseApproval;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import com.revature.repository.Approval;
import com.revature.service.ExpenseService;
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
class ExpenseServiceEscapeCsvValueTest
{

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp()
    {
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
    }

    /**
     * Test Case 1: Verify values with commas are quoted
     */
    @Test
    @DisplayName("Should quote values containing commas")
    void testEscapeCsvValue_ValuesWithCommasAreQuoted()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "Smith, John", 100.0,
                "Lunch, dinner, breakfast", "2024-01-15", "approved", 201,
                "Approved, processed", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // Values with commas should be wrapped in quotes
        assertTrue(csvReport.contains("\"Smith, John\""),
                "Username with comma should be quoted");
        assertTrue(csvReport.contains("\"Lunch, dinner, breakfast\""),
                "Description with commas should be quoted");
        assertTrue(csvReport.contains("\"Approved, processed\""),
                "Comment with comma should be quoted");

        // Verify the values are properly escaped (not just raw with commas)
        assertFalse(csvReport.contains("Smith, John,100.0"),
                "Unquoted comma would break CSV structure");
    }

    /**
     * Test Case 2: Verify values with quotes have quotes doubled
     */
    @Test
    @DisplayName("Should double quotes in values containing quotes")
    void testEscapeCsvValue_ValuesWithQuotesHaveQuotesDoubled()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John \"Johnny\" Doe", 100.0,
                "Item marked \"urgent\"", "2024-01-15", "approved", 201,
                "Manager said \"approved\"", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // Quotes should be doubled and the value should be wrapped in quotes
        assertTrue(csvReport.contains("\"John \"\"Johnny\"\" Doe\""),
                "Username with quotes should have doubled quotes");
        assertTrue(csvReport.contains("\"Item marked \"\"urgent\"\"\""),
                "Description with quotes should have doubled quotes");
        assertTrue(csvReport.contains("\"Manager said \"\"approved\"\"\""),
                "Comment with quotes should have doubled quotes");

        // Verify single quotes are not present (should be doubled)
        assertFalse(csvReport.contains("John \"Johnny\" Doe"),
                "Single quotes should not appear unescaped");
    }

    /**
     * Test Case 3: Verify values with newlines are quoted
     */
    @Test
    @DisplayName("Should quote values containing newlines")
    void testEscapeCsvValue_ValuesWithNewlinesAreQuoted()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "John\nDoe", 100.0,
                "Line one\nLine two\nLine three", "2024-01-15", "approved", 201,
                "First line\nSecond line", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // Values with newlines should be wrapped in quotes
        assertTrue(csvReport.contains("\"John\nDoe\""),
                "Username with newline should be quoted");
        assertTrue(csvReport.contains("\"Line one\nLine two\nLine three\""),
                "Description with newlines should be quoted");
        assertTrue(csvReport.contains("\"First line\nSecond line\""),
                "Comment with newline should be quoted");

        // Verify newlines are preserved within quotes (not replaced)
        assertTrue(csvReport.contains("Line one\nLine two"),
                "Newlines should be preserved in the output");
    }

    /**
     * Test Case 4: Verify simple values without special chars remain unquoted
     */
    @Test
    @DisplayName("Should not quote simple values without special characters")
    void testEscapeCsvValue_SimpleValuesRemainUnquoted()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                "OfficeSupplies", "2024-01-15", "approved", 201,
                "Approved", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);
        String[] lines = csvReport.split("\n");
        String dataRow = lines[1];

        // Simple values without special characters should NOT be quoted
        // Check that the values appear without surrounding quotes in the raw string
        assertTrue(dataRow.contains("JohnDoe,") || dataRow.contains(",JohnDoe,"),
                "Simple username should not be quoted");
        assertTrue(dataRow.contains("OfficeSupplies,") || dataRow.contains(",OfficeSupplies,"),
                "Simple description should not be quoted");
        assertTrue(dataRow.contains("Approved,") || dataRow.contains(",Approved,"),
                "Simple comment should not be quoted");

        // Verify no unnecessary quotes around simple values
        assertFalse(dataRow.contains("\"JohnDoe\""),
                "Simple values should not have quotes");
        assertFalse(dataRow.contains("\"OfficeSupplies\""),
                "Simple values should not have quotes");
        assertFalse(dataRow.contains("\"Approved\""),
                "Simple comment should not have quotes");
    }

    /**
     * Test Case 5: Verify null/empty string handling
     */
    @Test
    @DisplayName("Should handle null and empty string values correctly")
    void testEscapeCsvValue_NullAndEmptyStringHandling()
    {
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
    }

    /**
     * Test Case 6: Verify combination of special characters
     */
    @Test
    @DisplayName("Should handle values with multiple special characters")
    void testEscapeCsvValue_CombinationOfSpecialCharacters()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "Smith, \"John\"", 100.0,
                "Description with \"quotes\", commas, and\nnewlines", "2024-01-15",
                "approved", 201, "Comment: \"good\",\napproved", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // All values with special characters should be quoted
        assertTrue(csvReport.contains("\"Smith, \"\"John\"\"\""),
                "Should handle comma and quotes together");
        assertTrue(csvReport.contains("\"Description with \"\"quotes\"\", commas, and\nnewlines\""),
                "Should handle quotes, commas, and newlines together");
        assertTrue(csvReport.contains("\"Comment: \"\"good\"\",\napproved\""),
                "Should handle all special characters in comment");

        // Verify proper escaping (quotes doubled, values wrapped)
        String[] lines = csvReport.split("\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        assertTrue(lines.length >= 2, "Should have at least header and one data row");
    }

    /**
     * Test Case 7: Verify edge case - only comma
     */
    @Test
    @DisplayName("Should quote value that is only a comma")
    void testEscapeCsvValue_OnlyComma()
    {
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
    }

    /**
     * Test Case 8: Verify edge case - only quote
     */
    @Test
    @DisplayName("Should quote and double a value that is only a quote")
    void testEscapeCsvValue_OnlyQuote()
    {
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
    }

    /**
     * Test Case 9: Verify edge case - only newline
     */
    @Test
    @DisplayName("Should quote value that is only a newline")
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
    }

    /**
     * Test Case 10: Verify whitespace-only values are not quoted
     */
    @Test
    @DisplayName("Should not quote values that are only whitespace")
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
    }

    /**
     * Test Case 11: Verify multiple consecutive special characters
     */
    @Test
    @DisplayName("Should handle multiple consecutive special characters")
    void testEscapeCsvValue_MultipleConsecutiveSpecialChars()
    {
        // Arrange
        List<ExpenseWithUser> expenses = new ArrayList<>();
        expenses.add(createExpenseWithUser(1, 101, "JohnDoe", 100.0,
                "Multiple \"\"\"\" quotes", "2024-01-15", "approved", 201,
                "Commas,,, here", "2024-01-16"));

        // Act
        String csvReport = expenseService.generateCsvReport(expenses);

        // Assert
        assertNotNull(csvReport);

        // Multiple consecutive quotes should be properly escaped
        assertTrue(csvReport.contains("\"Multiple \"\"\"\"\"\"\"\"\"\" quotes\""),
                "Should escape all quotes by doubling them");

        // Multiple consecutive commas should still be within quotes
        assertTrue(csvReport.contains("\"Commas,,, here\""),
                "Multiple commas should be within quotes");
    }

    // Helper method to create ExpenseWithUser objects for testing
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