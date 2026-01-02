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
import static org.mockito.Mockito.*;

/**
 * Test cases for ExpenseService.getExpensesByCategory() method
 * User Story 1.5 - Test ExpenseService.getExpensesByCategory()
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceCategoryTest
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

    @Test
    @DisplayName("Should return expenses with descriptions containing category text")
    void testGetExpensesByCategory_MatchesDescriptionsContainingCategoryText() {
        // Arrange
        String category = "Grocery";

        List<ExpenseWithUser> mockExpenses = new ArrayList<>();
        mockExpenses.add(createExpenseWithUser(1, 1, 50.0, "Groceries at Walmart", "2024-01-15"));
        mockExpenses.add(createExpenseWithUser(2, 2, 75.0, "Grocery shopping", "2024-01-16"));

        when(expenseRepository.findExpensesByCategory(category)).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getExpensesByCategory(category);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e ->
                e.getExpense().getDescription().contains("Groceries at Walmart")));
        assertTrue(result.stream().anyMatch(e ->
                e.getExpense().getDescription().contains("Grocery shopping")));

        verify(expenseRepository, times(1)).findExpensesByCategory(category);
    }

    @Test
    @DisplayName("Should handle case sensitivity correctly")
    void testGetExpensesByCategory_CaseSensitivityBehavior() {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();
        mockExpenses.add(createExpenseWithUser(1, 1, 100.0, "TRAVEL expenses", "2024-01-15"));
        mockExpenses.add(createExpenseWithUser(2, 2, 200.0, "travel booking", "2024-01-16"));
        mockExpenses.add(createExpenseWithUser(3, 3, 150.0, "Business Travel", "2024-01-17"));

        // Mock repository to return same results for different cases (case-insensitive behavior)
        when(expenseRepository.findExpensesByCategory("travel")).thenReturn(mockExpenses);
        when(expenseRepository.findExpensesByCategory("TRAVEL")).thenReturn(mockExpenses);
        when(expenseRepository.findExpensesByCategory("Travel")).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> resultLower = expenseService.getExpensesByCategory("travel");
        List<ExpenseWithUser> resultUpper = expenseService.getExpensesByCategory("TRAVEL");
        List<ExpenseWithUser> resultMixed = expenseService.getExpensesByCategory("Travel");

        // Assert
        assertNotNull(resultLower);
        assertNotNull(resultUpper);
        assertNotNull(resultMixed);

        // All three should return the same number of results (case-insensitive)
        assertEquals(3, resultLower.size());
        assertEquals(3, resultUpper.size());
        assertEquals(3, resultMixed.size());

        verify(expenseRepository, times(1)).findExpensesByCategory("travel");
        verify(expenseRepository, times(1)).findExpensesByCategory("TRAVEL");
        verify(expenseRepository, times(1)).findExpensesByCategory("Travel");
    }

    @Test
    @DisplayName("TC-1.5-003: Should return expenses with partial substring matches")
    void testGetExpensesByCategory_PartialMatchesWork() {
        // Arrange
        String category = "care";

        List<ExpenseWithUser> mockExpenses = new ArrayList<>();
        mockExpenses.add(createExpenseWithUser(1, 1, 300.0, "Healthcare premium", "2024-01-15"));
        mockExpenses.add(createExpenseWithUser(2, 2, 150.0, "Skincare products", "2024-01-16"));

        when(expenseRepository.findExpensesByCategory(category)).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getExpensesByCategory(category);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify both prefix and suffix matches
        assertTrue(result.stream().anyMatch(e ->
                e.getExpense().getDescription().contains("Healthcare")));
        assertTrue(result.stream().anyMatch(e ->
                e.getExpense().getDescription().contains("Skincare")));

        // Verify that descriptions actually contain the substring "care"
        result.forEach(expense ->
                assertTrue(expense.getExpense().getDescription().toLowerCase().contains(category.toLowerCase()),
                        "Description should contain 'care': " + expense.getExpense().getDescription()));

        verify(expenseRepository, times(1)).findExpensesByCategory(category);
    }

    @Test
    @DisplayName("Should return empty list when no expenses match category")
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
        List<ExpenseWithUser> resultPercent = expenseService.getExpensesByCategory(categoryWithPercent);
        List<ExpenseWithUser> resultUnderscore = expenseService.getExpensesByCategory(categoryWithUnderscore);
        List<ExpenseWithUser> resultDash = expenseService.getExpensesByCategory(categoryWithDash);
        List<ExpenseWithUser> resultApostrophe = expenseService.getExpensesByCategory(categoryWithApostrophe);

        // Assert
        assertNotNull(resultPercent);
        assertEquals(1, resultPercent.size());
        assertTrue(resultPercent.get(0).getExpense().getDescription().contains("50%"));

        assertNotNull(resultUnderscore);
        assertEquals(1, resultUnderscore.size());
        assertTrue(resultUnderscore.get(0).getExpense().getDescription().contains("IT_support"));

        assertNotNull(resultDash);
        assertEquals(1, resultDash.size());
        assertTrue(resultDash.get(0).getExpense().getDescription().contains("Co-working"));

        assertNotNull(resultApostrophe);
        assertEquals(1, resultApostrophe.size());
        assertTrue(resultApostrophe.get(0).getExpense().getDescription().contains("Client's"));

        verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithPercent);
        verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithUnderscore);
        verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithDash);
        verify(expenseRepository, times(1)).findExpensesByCategory(categoryWithApostrophe);
    }

    /**
     * Bonus Test: Verify null or empty category handling
     */
    @Test
    @DisplayName("Should handle null category gracefully")
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
    }

    /**
     * Bonus Test: Verify empty string category handling
     */
    @Test
    @DisplayName("Should handle empty string category")
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
    }

    // Helper method to create ExpenseWithUser objects for testing
    private ExpenseWithUser createExpenseWithUser(int expenseId, int userId, double amount,
                                                  String description, String date) {
        Expense expense = new Expense(expenseId, userId, amount, description, date);
        User user = new User(); // You may need to set user properties if needed
        user.setId(userId);
        user.setUsername("user" + userId);

        Approval approval = new Approval(); // You may need to create this class or mock it
        approval.setStatus("pending");

        return new ExpenseWithUser(expense, user, approval);
    }
}