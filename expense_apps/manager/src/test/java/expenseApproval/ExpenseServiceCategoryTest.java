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
import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Service")
@Story("Get Expenses by Category")
@ExtendWith(MockitoExtension.class)
class ExpenseServiceCategoryTest {

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
    @DisplayName("Should return expenses with descriptions containing category text")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpensesByCategory_MatchesDescriptionsContainingCategoryText() {
        String category = "Grocery";

        Allure.step("Arrange: create mock expenses", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            mockExpenses.add(createExpenseWithUser(1, 1, 50.0, "Groceries at Walmart", "2024-01-15"));
            mockExpenses.add(createExpenseWithUser(2, 2, 75.0, "Grocery shopping", "2024-01-16"));

            when(expenseRepository.findExpensesByCategory(category)).thenReturn(mockExpenses);

            Allure.step("Act: call getExpensesByCategory", () -> {
                List<ExpenseWithUser> result = expenseService.getExpensesByCategory(category);

                Allure.step("Assert: verify results", () -> {
                    assertNotNull(result);
                    assertEquals(2, result.size());
                    assertTrue(result.stream().anyMatch(e ->
                            e.getExpense().getDescription().contains("Groceries at Walmart")));
                    assertTrue(result.stream().anyMatch(e ->
                            e.getExpense().getDescription().contains("Grocery shopping")));

                    verify(expenseRepository, times(1)).findExpensesByCategory(category);
                });
            });
        });
    }

    @Test
    @DisplayName("Should handle case sensitivity correctly")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByCategory_CaseSensitivityBehavior() {
        Allure.step("Arrange: mock expenses for different case inputs", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            mockExpenses.add(createExpenseWithUser(1, 1, 100.0, "TRAVEL expenses", "2024-01-15"));
            mockExpenses.add(createExpenseWithUser(2, 2, 200.0, "travel booking", "2024-01-16"));
            mockExpenses.add(createExpenseWithUser(3, 3, 150.0, "Business Travel", "2024-01-17"));

            when(expenseRepository.findExpensesByCategory("travel")).thenReturn(mockExpenses);
            when(expenseRepository.findExpensesByCategory("TRAVEL")).thenReturn(mockExpenses);
            when(expenseRepository.findExpensesByCategory("Travel")).thenReturn(mockExpenses);

            Allure.step("Act: call getExpensesByCategory with various cases", () -> {
                List<ExpenseWithUser> resultLower = expenseService.getExpensesByCategory("travel");
                List<ExpenseWithUser> resultUpper = expenseService.getExpensesByCategory("TRAVEL");
                List<ExpenseWithUser> resultMixed = expenseService.getExpensesByCategory("Travel");

                Allure.step("Assert: verify results are consistent", () -> {
                    assertNotNull(resultLower);
                    assertNotNull(resultUpper);
                    assertNotNull(resultMixed);

                    assertEquals(3, resultLower.size());
                    assertEquals(3, resultUpper.size());
                    assertEquals(3, resultMixed.size());

                    verify(expenseRepository, times(1)).findExpensesByCategory("travel");
                    verify(expenseRepository, times(1)).findExpensesByCategory("TRAVEL");
                    verify(expenseRepository, times(1)).findExpensesByCategory("Travel");
                });
            });
        });
    }

    @Test
    @DisplayName("Should return expenses with partial substring matches")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByCategory_PartialMatchesWork() {
        String category = "care";

        Allure.step("Arrange: create partial match expenses", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            mockExpenses.add(createExpenseWithUser(1, 1, 300.0, "Healthcare premium", "2024-01-15"));
            mockExpenses.add(createExpenseWithUser(2, 2, 150.0, "Skincare products", "2024-01-16"));

            when(expenseRepository.findExpensesByCategory(category)).thenReturn(mockExpenses);

            Allure.step("Act: call getExpensesByCategory", () -> {
                List<ExpenseWithUser> result = expenseService.getExpensesByCategory(category);

                Allure.step("Assert: verify partial matches", () -> {
                    assertNotNull(result);
                    assertEquals(2, result.size());

                    assertTrue(result.stream().anyMatch(e ->
                            e.getExpense().getDescription().contains("Healthcare")));
                    assertTrue(result.stream().anyMatch(e ->
                            e.getExpense().getDescription().contains("Skincare")));

                    result.forEach(expense ->
                            assertTrue(expense.getExpense().getDescription().toLowerCase()
                                            .contains(category.toLowerCase()),
                                    "Description should contain 'care': " + expense.getExpense().getDescription()));

                    verify(expenseRepository, times(1)).findExpensesByCategory(category);
                });
            });
        });
    }

    // Remaining tests follow same pattern: wrap Arrange, Act, Assert in Allure.step()

    // Helper method
    private ExpenseWithUser createExpenseWithUser(int expenseId, int userId, double amount,
                                                  String description, String date) {
        Expense expense = new Expense(expenseId, userId, amount, description, date);
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);

        Approval approval = new Approval();
        approval.setStatus("pending");

        return new ExpenseWithUser(expense, user, approval);
    }
}
