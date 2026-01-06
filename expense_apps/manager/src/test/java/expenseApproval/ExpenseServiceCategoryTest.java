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

        @Test
        @DisplayName("Should return empty list when no expenses match category")
        @Description("Verify that the service returns an empty list when searching for a category that has no associated expenses")
        @Severity(SeverityLevel.NORMAL)
        @Story("Category Search - Edge Cases")
        void testGetExpensesByCategory_EmptyListForNonMatchingCategory() {
            // Arrange
            String category = "xyz123";
            List<ExpenseWithUser> emptyList = new ArrayList<>();

            when(expenseRepository.findExpensesByCategory(category)).thenReturn(emptyList);

            // Act
            List<ExpenseWithUser> result = expenseService.getExpensesByCategory(category);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.size());

            verify(expenseRepository, times(1)).findExpensesByCategory(category);
        }

        @Test
        @DisplayName("Should handle special characters in category string")
        @Description("Test that the service correctly handles categories containing special characters like %, _, -, and apostrophes")
        @Severity(SeverityLevel.CRITICAL)
        @Story("Category Search - Special Characters")
        void testGetExpensesByCategory_SpecialCharactersInCategory() {
            // Arrange - Test various special characters
            String categoryWithPercent = "50%";
            String categoryWithUnderscore = "IT_support";
            String categoryWithDash = "Co-working";
            String categoryWithApostrophe = "Client's";

            List<ExpenseWithUser> mockExpensesPercent = new ArrayList<>();
            mockExpensesPercent.add(createExpenseWithUser(1, 1, 100.0, "50% discount applied", "2024-01-15"));

            List<ExpenseWithUser> mockExpensesUnderscore = new ArrayList<>();
            mockExpensesUnderscore.add(createExpenseWithUser(2, 2, 200.0, "IT_support ticket #123", "2024-01-16"));

            List<ExpenseWithUser> mockExpensesDash = new ArrayList<>();
            mockExpensesDash.add(createExpenseWithUser(3, 3, 300.0, "Co-working space rental", "2024-01-17"));

            List<ExpenseWithUser> mockExpensesApostrophe = new ArrayList<>();
            mockExpensesApostrophe.add(createExpenseWithUser(4, 4, 150.0, "Client's dinner meeting", "2024-01-18"));

            when(expenseRepository.findExpensesByCategory(categoryWithPercent)).thenReturn(mockExpensesPercent);
            when(expenseRepository.findExpensesByCategory(categoryWithUnderscore)).thenReturn(mockExpensesUnderscore);
            when(expenseRepository.findExpensesByCategory(categoryWithDash)).thenReturn(mockExpensesDash);
            when(expenseRepository.findExpensesByCategory(categoryWithApostrophe)).thenReturn(mockExpensesApostrophe);

            // Act
            testCategoryWithSpecialCharacter(categoryWithPercent, mockExpensesPercent, "50%");
            testCategoryWithSpecialCharacter(categoryWithUnderscore, mockExpensesUnderscore, "IT_support");
            testCategoryWithSpecialCharacter(categoryWithDash, mockExpensesDash, "Co-working");
            testCategoryWithSpecialCharacter(categoryWithApostrophe, mockExpensesApostrophe, "Client's");

            // Verify
            verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithPercent);
            verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithUnderscore);
            verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithDash);
            verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithApostrophe);
        }

        @Step("Test category '{category}' with expected text '{expectedText}'")
        private void testCategoryWithSpecialCharacter(String category, List<ExpenseWithUser> mockExpenses, String expectedText) {
            List<ExpenseWithUser> result = expenseService.getExpensesByCategory(category);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.get(0).getExpense().getDescription().contains(expectedText));

            Allure.addAttachment("Category", category);
            Allure.addAttachment("Result Count", String.valueOf(result.size()));
        }

        /**
         * Bonus Test: Verify null or empty category handling
         */
        @Test
        @DisplayName("Should handle null category gracefully")
        @Description("Verify that the service handles null category input without throwing exceptions")
        @Severity(SeverityLevel.CRITICAL)
        @Story("Category Search - Null Handling")
        @Issue("EXPENSE-101")
        void testGetExpensesByCategory_NullCategory() {
            // Arrange
            String nullCategory = null;
            List<ExpenseWithUser> emptyList = new ArrayList<>();

            when(expenseRepository.findExpensesByCategory(nullCategory)).thenReturn(emptyList);

            // Act
            List<ExpenseWithUser> result = expenseService.getExpensesByCategory(nullCategory);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(expenseRepository, times(1)).findExpensesByCategory(nullCategory);

            Allure.step("Verified null category returns empty list");
        }

        /**
         * Bonus Test: Verify empty string category handling
         */
        @Test
        @DisplayName("Should handle empty string category")
        @Description("Verify that the service handles empty string category input correctly")
        @Severity(SeverityLevel.NORMAL)
        @Story("Category Search - Empty String Handling")
        @Issue("EXPENSE-102")
        void testGetExpensesByCategory_EmptyStringCategory() {
            // Arrange
            String emptyCategory = "";
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();

            when(expenseRepository.findExpensesByCategory(emptyCategory)).thenReturn(mockExpenses);

            // Act
            List<ExpenseWithUser> result = expenseService.getExpensesByCategory(emptyCategory);

            // Assert
            assertNotNull(result);

            verify(expenseRepository, times(1)).findExpensesByCategory(emptyCategory);

            Allure.step("Verified empty string category is handled");
        }

        // Helper method to create ExpenseWithUser objects for testing
        @Step("Create test expense with ID: {expenseId}, Amount: {amount}")
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
