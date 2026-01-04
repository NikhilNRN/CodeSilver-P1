package expenseApproval;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Manager App")
@Feature("Expense Service")
@Story("CSV Report Generation")
@ExtendWith(MockitoExtension.class)
class ExpenseServiceCsvReportTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    private ExpenseService expenseService;

    @BeforeEach
    @Step("Initialize ExpenseService with mocked repositories")
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
    }

    @Test
    @DisplayName("Should generate correct CSV header row format")
    @Severity(SeverityLevel.CRITICAL)
    void testGenerateCsvReport_HeaderRowFormat() {
        Allure.step("Arrange: create mock expenses", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                    "Office supplies", "2024-01-15", "approved", 201,
                    "Looks good", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: validate header row format", () -> {
                    assertNotNull(csvReport);
                    String[] lines = csvReport.split("\n");
                    assertTrue(lines.length >= 1, "CSV should have at least header row");

                    String header = lines[0];
                    assertEquals("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date",
                            header, "Header format should match expected format");

                    String[] columns = header.split(",");
                    assertEquals(9, columns.length, "Header should have 9 columns");
                    assertEquals("Expense ID", columns[0]);
                    assertEquals("Employee", columns[1]);
                    assertEquals("Amount", columns[2]);
                    assertEquals("Description", columns[3]);
                    assertEquals("Date", columns[4]);
                    assertEquals("Status", columns[5]);
                    assertEquals("Reviewer", columns[6]);
                    assertEquals("Comment", columns[7]);
                    assertEquals("Review Date", columns[8]);
                });
            });
        });
    }

    @Test
    @DisplayName("Should generate correct CSV data rows format")
    @Severity(SeverityLevel.CRITICAL)
    void testGenerateCsvReport_DataRowsFormat() {
        Allure.step("Arrange: create multiple mock expenses", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.50,
                    "Office supplies", "2024-01-15", "approved", 201,
                    "Approved", "2024-01-16"));
            expenses.add(createExpenseWithUser(2, 102, "Jane Smith", 250.75,
                    "Travel expenses", "2024-01-20", "pending", null,
                    null, null));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: validate data row format", () -> {
                    assertNotNull(csvReport);
                    String[] lines = csvReport.split("\n");
                    assertEquals(3, lines.length, "CSV should have header + 2 data rows");

                    // First row
                    String dataRow1 = lines[1];
                    assertTrue(dataRow1.contains("1,"));
                    assertTrue(dataRow1.contains("John Doe"));
                    assertTrue(dataRow1.contains("100.5"));
                    assertTrue(dataRow1.contains("Office supplies"));
                    assertTrue(dataRow1.contains("2024-01-15"));
                    assertTrue(dataRow1.contains("approved"));
                    assertTrue(dataRow1.contains("201"));
                    assertTrue(dataRow1.contains("Approved"));
                    assertTrue(dataRow1.contains("2024-01-16"));

                    // Second row (null handling)
                    String dataRow2 = lines[2];
                    assertTrue(dataRow2.contains("2,"));
                    assertTrue(dataRow2.contains("Jane Smith"));
                    assertTrue(dataRow2.contains("250.75"));
                    assertTrue(dataRow2.contains("pending"));
                    long commaCount = dataRow2.chars().filter(ch -> ch == ',').count();
                    assertEquals(8, commaCount, "Should have 8 commas for 9 columns");
                });
            });
        });
    }

    @Test
    @DisplayName("Should properly escape commas in CSV values")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateCsvReport_CommaEscaping() {
        Allure.step("Arrange: create expenses with commas", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "Doe, John", 100.0,
                    "Office supplies, paper, pens", "2024-01-15", "approved", 201,
                    "Approved, looks good", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: validate comma escaping", () -> {
                    assertNotNull(csvReport);
                    String dataRow = csvReport.split("\n")[1];
                    assertTrue(dataRow.contains("\"Doe, John\""));
                    assertTrue(dataRow.contains("\"Office supplies, paper, pens\""));
                    assertTrue(dataRow.contains("\"Approved, looks good\""));
                    assertFalse(dataRow.matches(".*,{2,}.*"), "Should not have consecutive commas");
                });
            });
        });
    }

    @Test
    @DisplayName("Should properly escape quotes in CSV values")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateCsvReport_QuoteEscaping() {
        Allure.step("Arrange: create expenses with quotes", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "John \"Johnny\" Doe", 100.0,
                    "Client said \"urgent\"", "2024-01-15", "approved", 201,
                    "Manager noted \"exceptional\"", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: validate quote escaping", () -> {
                    assertNotNull(csvReport);
                    String dataRow = csvReport.split("\n")[1];
                    assertTrue(dataRow.contains("\"John \"\"Johnny\"\" Doe\""));
                    assertTrue(dataRow.contains("\"Client said \"\"urgent\"\"\""));
                    assertTrue(dataRow.contains("\"Manager noted \"\"exceptional\"\"\""));
                });
            });
        });
    }

    @Test
    @DisplayName("Should properly handle newlines in CSV values")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateCsvReport_NewlineHandling() {
        Allure.step("Arrange: create expenses with newlines", () -> {
            List<ExpenseWithUser> expenses = new ArrayList<>();
            expenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0,
                    "Line 1\nLine 2\nLine 3", "2024-01-15", "approved", 201,
                    "Comment line 1\nComment line 2", "2024-01-16"));

            Allure.step("Act: generate CSV report", () -> {
                String csvReport = expenseService.generateCsvReport(expenses);

                Allure.step("Assert: validate newline handling", () -> {
                    assertNotNull(csvReport);
                    assertTrue(csvReport.contains("\"Line 1\nLine 2\nLine 3\""));
                    assertTrue(csvReport.contains("\"Comment line 1\nComment line 2\""));
                });
            });
        });
    }

    // Similar pattern can be applied for remaining tests (empty list, special chars, nulls, multiple rows)

    // Helper method
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
