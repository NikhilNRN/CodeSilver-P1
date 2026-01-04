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
import static org.mockito.Mockito.*;

/**
 * Test cases for ExpenseService.getAllExpenses() method
 */
@ExtendWith(MockitoExtension.class)
@Epic("Expense Management")
@Feature("Retrieve All Expenses")
class ExpenseServiceGetAllTest {

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
    @DisplayName("Should return all expenses with complete user information")
    @Story("Retrieve all expenses with full user info")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpenses_ReturnsAllExpensesWithUserInfo() {
        Allure.step("Arrange: prepare mock expenses with user info", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            mockExpenses.add(createExpenseWithUser(1, 101, "John Doe", 100.0, "Office supplies", "2024-01-15", "pending"));
            mockExpenses.add(createExpenseWithUser(2, 102, "Jane Smith", 200.0, "Travel expenses", "2024-01-16", "approved"));
            mockExpenses.add(createExpenseWithUser(3, 103, "Bob Johnson", 150.0, "Client dinner", "2024-01-17", "denied"));

            when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

            Allure.step("Act: call getAllExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getAllExpenses();

                Allure.step("Assert: verify expenses and user info", () -> {
                    assertNotNull(result);
                    assertEquals(3, result.size());
                    result.forEach(expenseWithUser -> {
                        assertNotNull(expenseWithUser.getExpense());
                        assertNotNull(expenseWithUser.getUser());
                        assertNotNull(expenseWithUser.getUser().getUsername());
                        assertTrue(expenseWithUser.getUser().getId() > 0);
                    });
                    assertTrue(result.stream().anyMatch(e -> e.getUser().getUsername().equals("John Doe")));
                    assertTrue(result.stream().anyMatch(e -> e.getUser().getUsername().equals("Jane Smith")));
                    assertTrue(result.stream().anyMatch(e -> e.getUser().getUsername().equals("Bob Johnson")));
                    verify(expenseRepository, times(1)).findAllExpensesWithUsers();
                });
            });
        });
    }

    @Test
    @DisplayName("Should include expenses with all approval statuses")
    @Story("Retrieve expenses covering all approval statuses")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpenses_IncludesAllApprovalStatuses() {
        Allure.step("Arrange: prepare mock expenses with varied statuses", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            mockExpenses.add(createExpenseWithUser(1, 101, "User1", 100.0, "Expense 1", "2024-01-15", "pending"));
            mockExpenses.add(createExpenseWithUser(2, 102, "User2", 200.0, "Expense 2", "2024-01-16", "pending"));
            mockExpenses.add(createExpenseWithUser(3, 103, "User3", 150.0, "Expense 3", "2024-01-17", "approved"));
            mockExpenses.add(createExpenseWithUser(4, 104, "User4", 300.0, "Expense 4", "2024-01-18", "approved"));
            mockExpenses.add(createExpenseWithUser(5, 105, "User5", 250.0, "Expense 5", "2024-01-19", "denied"));
            mockExpenses.add(createExpenseWithUser(6, 106, "User6", 175.0, "Expense 6", "2024-01-20", "denied"));

            when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

            Allure.step("Act: call getAllExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getAllExpenses();

                Allure.step("Assert: verify all approval statuses present", () -> {
                    assertNotNull(result);
                    assertEquals(6, result.size());
                    assertEquals(2, result.stream().filter(e -> "pending".equals(e.getApproval().getStatus())).count());
                    assertEquals(2, result.stream().filter(e -> "approved".equals(e.getApproval().getStatus())).count());
                    assertEquals(2, result.stream().filter(e -> "denied".equals(e.getApproval().getStatus())).count());
                    verify(expenseRepository, times(1)).findAllExpensesWithUsers();
                });
            });
        });
    }

    @Test
    @DisplayName("Should return expenses in correct date order")
    @Story("Retrieve expenses in chronological order")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpenses_CorrectOrderingByDate() {
        Allure.step("Arrange: prepare mock expenses in ascending date order", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            mockExpenses.add(createExpenseWithUser(1, 101, "User1", 100.0, "Expense 1", "2024-01-10", "approved"));
            mockExpenses.add(createExpenseWithUser(2, 102, "User2", 200.0, "Expense 2", "2024-01-15", "pending"));
            mockExpenses.add(createExpenseWithUser(3, 103, "User3", 150.0, "Expense 3", "2024-01-20", "denied"));
            mockExpenses.add(createExpenseWithUser(4, 104, "User4", 300.0, "Expense 4", "2024-01-25", "approved"));
            mockExpenses.add(createExpenseWithUser(5, 105, "User5", 250.0, "Expense 5", "2024-01-30", "pending"));

            when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

            Allure.step("Act: call getAllExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getAllExpenses();

                Allure.step("Assert: verify ascending date order", () -> {
                    assertNotNull(result);
                    assertEquals(5, result.size());
                    for (int i = 0; i < result.size() - 1; i++) {
                        String currentDate = result.get(i).getExpense().getDate();
                        String nextDate = result.get(i + 1).getExpense().getDate();
                        assertTrue(currentDate.compareTo(nextDate) <= 0,
                                String.format("Dates should be ascending: %s before %s", currentDate, nextDate));
                    }
                    verify(expenseRepository, times(1)).findAllExpensesWithUsers();
                });
            });
        });
    }

    @Test
    @DisplayName("Should return empty list when no expenses exist")
    @Story("Handle empty expense list")
    @Severity(SeverityLevel.MINOR)
    void testGetAllExpenses_EmptyList() {
        Allure.step("Arrange: empty repository", () -> {
            when(expenseRepository.findAllExpensesWithUsers()).thenReturn(new ArrayList<>());

            Allure.step("Act: call getAllExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getAllExpenses();

                Allure.step("Assert: verify empty list", () -> {
                    assertNotNull(result);
                    assertTrue(result.isEmpty());
                    verify(expenseRepository, times(1)).findAllExpensesWithUsers();
                });
            });
        });
    }

    @Step("Create ExpenseWithUser object")
    private ExpenseWithUser createExpenseWithUser(int expenseId, int userId, String username,
                                                  double amount, String description,
                                                  String date, String status) {
        Expense expense = new Expense(expenseId, userId, amount, description, date);

        User user = new User();
        user.setId(userId);
        user.setUsername(username);

        Approval approval = new Approval();
        approval.setExpenseId(expenseId);
        approval.setStatus(status);

        return new ExpenseWithUser(expense, user, approval);
    }
}
