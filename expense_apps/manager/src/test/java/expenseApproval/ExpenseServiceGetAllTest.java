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
 * Test cases for ExpenseService.getAllExpenses() method
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceGetAllTest
{

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
    }

    /**
     * Test Case 1: Verify returns all expenses with user information
     */
    @Test
    @DisplayName("Should return all expenses with complete user information")
    void testGetAllExpenses_ReturnsAllExpensesWithUserInfo() {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        // Create expenses with different users
        mockExpenses.add(createExpenseWithUser(1, 101, "John Doe",
                100.0, "Office supplies", "2024-01-15", "pending"));
        mockExpenses.add(createExpenseWithUser(2, 102, "Jane Smith",
                200.0, "Travel expenses", "2024-01-16", "approved"));
        mockExpenses.add(createExpenseWithUser(3, 103, "Bob Johnson",
                150.0, "Client dinner", "2024-01-17", "denied"));

        when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Verify all expenses have user information
        result.forEach(expenseWithUser -> {
            assertNotNull(expenseWithUser.getExpense(), "Expense should not be null");
            assertNotNull(expenseWithUser.getUser(), "User should not be null");
            assertNotNull(expenseWithUser.getUser().getUsername(), "Username should not be null");
            assertTrue(expenseWithUser.getUser().getId() > 0, "User ID should be valid");
        });

        // Verify specific user information is present
        assertTrue(result.stream().anyMatch(e ->
                e.getUser().getUsername().equals("John Doe")));
        assertTrue(result.stream().anyMatch(e ->
                e.getUser().getUsername().equals("Jane Smith")));
        assertTrue(result.stream().anyMatch(e ->
                e.getUser().getUsername().equals("Bob Johnson")));

        verify(expenseRepository, times(1)).findAllExpensesWithUsers();
    }

    /**
     * Test Case 2: Verify includes all approval statuses (pending, approved, denied)
     */
    @Test
    @DisplayName("Should include expenses with all approval statuses")
    void testGetAllExpenses_IncludesAllApprovalStatuses() {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        // Create expenses with different approval statuses
        mockExpenses.add(createExpenseWithUser(1, 101, "User1",
                100.0, "Expense 1", "2024-01-15", "pending"));
        mockExpenses.add(createExpenseWithUser(2, 102, "User2",
                200.0, "Expense 2", "2024-01-16", "pending"));
        mockExpenses.add(createExpenseWithUser(3, 103, "User3",
                150.0, "Expense 3", "2024-01-17", "approved"));
        mockExpenses.add(createExpenseWithUser(4, 104, "User4",
                300.0, "Expense 4", "2024-01-18", "approved"));
        mockExpenses.add(createExpenseWithUser(5, 105, "User5",
                250.0, "Expense 5", "2024-01-19", "denied"));
        mockExpenses.add(createExpenseWithUser(6, 106, "User6",
                175.0, "Expense 6", "2024-01-20", "denied"));

        when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(6, result.size());

        // Count expenses by status
        long pendingCount = result.stream()
                .filter(e -> "pending".equals(e.getApproval().getStatus()))
                .count();
        long approvedCount = result.stream()
                .filter(e -> "approved".equals(e.getApproval().getStatus()))
                .count();
        long deniedCount = result.stream()
                .filter(e -> "denied".equals(e.getApproval().getStatus()))
                .count();

        // Verify all three statuses are present
        assertEquals(2, pendingCount, "Should have 2 pending expenses");
        assertEquals(2, approvedCount, "Should have 2 approved expenses");
        assertEquals(2, deniedCount, "Should have 2 denied expenses");

        // Verify at least one of each status exists
        assertTrue(result.stream().anyMatch(e ->
                        "pending".equals(e.getApproval().getStatus())),
                "Should include pending expenses");
        assertTrue(result.stream().anyMatch(e ->
                        "approved".equals(e.getApproval().getStatus())),
                "Should include approved expenses");
        assertTrue(result.stream().anyMatch(e ->
                        "denied".equals(e.getApproval().getStatus())),
                "Should include denied expenses");

        verify(expenseRepository, times(1)).findAllExpensesWithUsers();
    }

    /**
     * Test Case 3: Verify correct ordering by date
     */
    @Test
    @DisplayName("Should return expenses in correct date order")
    void testGetAllExpenses_CorrectOrderingByDate() {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        // Create expenses in chronological order (oldest to newest)
        mockExpenses.add(createExpenseWithUser(1, 101, "User1",
                100.0, "Expense 1", "2024-01-10", "approved"));
        mockExpenses.add(createExpenseWithUser(2, 102, "User2",
                200.0, "Expense 2", "2024-01-15", "pending"));
        mockExpenses.add(createExpenseWithUser(3, 103, "User3",
                150.0, "Expense 3", "2024-01-20", "denied"));
        mockExpenses.add(createExpenseWithUser(4, 104, "User4",
                300.0, "Expense 4", "2024-01-25", "approved"));
        mockExpenses.add(createExpenseWithUser(5, 105, "User5",
                250.0, "Expense 5", "2024-01-30", "pending"));

        when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());

        // Verify dates are in correct order (ascending)
        for (int i = 0; i < result.size() - 1; i++) {
            String currentDate = result.get(i).getExpense().getDate();
            String nextDate = result.get(i + 1).getExpense().getDate();

            assertTrue(currentDate.compareTo(nextDate) <= 0,
                    String.format("Dates should be in ascending order: %s should come before or equal to %s",
                            currentDate, nextDate));
        }

        // Verify specific date ordering
        assertEquals("2024-01-10", result.get(0).getExpense().getDate());
        assertEquals("2024-01-15", result.get(1).getExpense().getDate());
        assertEquals("2024-01-20", result.get(2).getExpense().getDate());
        assertEquals("2024-01-25", result.get(3).getExpense().getDate());
        assertEquals("2024-01-30", result.get(4).getExpense().getDate());

        verify(expenseRepository, times(1)).findAllExpensesWithUsers();
    }

    /**
     * Test Case 4: Verify correct ordering by date (descending order scenario)
     */
    @Test
    @DisplayName("Should return expenses in correct date order (descending)")
    void testGetAllExpenses_CorrectOrderingByDate_Descending() {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        // Create expenses in reverse chronological order (newest to oldest)
        mockExpenses.add(createExpenseWithUser(5, 105, "User5",
                250.0, "Expense 5", "2024-01-30", "pending"));
        mockExpenses.add(createExpenseWithUser(4, 104, "User4",
                300.0, "Expense 4", "2024-01-25", "approved"));
        mockExpenses.add(createExpenseWithUser(3, 103, "User3",
                150.0, "Expense 3", "2024-01-20", "denied"));
        mockExpenses.add(createExpenseWithUser(2, 102, "User2",
                200.0, "Expense 2", "2024-01-15", "pending"));
        mockExpenses.add(createExpenseWithUser(1, 101, "User1",
                100.0, "Expense 1", "2024-01-10", "approved"));

        when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());

        // Verify dates are in correct order (descending)
        for (int i = 0; i < result.size() - 1; i++) {
            String currentDate = result.get(i).getExpense().getDate();
            String nextDate = result.get(i + 1).getExpense().getDate();

            assertTrue(currentDate.compareTo(nextDate) >= 0,
                    String.format("Dates should be in descending order: %s should come after or equal to %s",
                            currentDate, nextDate));
        }

        // Verify specific date ordering
        assertEquals("2024-01-30", result.get(0).getExpense().getDate());
        assertEquals("2024-01-25", result.get(1).getExpense().getDate());
        assertEquals("2024-01-20", result.get(2).getExpense().getDate());
        assertEquals("2024-01-15", result.get(3).getExpense().getDate());
        assertEquals("2024-01-10", result.get(4).getExpense().getDate());

        verify(expenseRepository, times(1)).findAllExpensesWithUsers();
    }

    /**
     * Bonus Test: Verify empty list when no expenses exist
     */
    @Test
    @DisplayName("Should return empty list when no expenses exist")
    void testGetAllExpenses_EmptyList() {
        // Arrange
        List<ExpenseWithUser> emptyList = new ArrayList<>();
        when(expenseRepository.findAllExpensesWithUsers()).thenReturn(emptyList);

        // Act
        List<ExpenseWithUser> result = expenseService.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());

        verify(expenseRepository, times(1)).findAllExpensesWithUsers();
    }

    /**
     * Bonus Test: Verify handles large number of expenses
     */
    @Test
    @DisplayName("Should handle large number of expenses")
    void testGetAllExpenses_LargeDataSet() {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        // Create 100 expenses
        for (int i = 1; i <= 100; i++) {
            String date = String.format("2024-01-%02d", (i % 28) + 1);
            String status = i % 3 == 0 ? "approved" : (i % 3 == 1 ? "pending" : "denied");
            mockExpenses.add(createExpenseWithUser(i, 100 + i, "User" + i,
                    i * 10.0, "Expense " + i, date, status));
        }

        when(expenseRepository.findAllExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.size());

        // Verify all have user information
        result.forEach(expenseWithUser -> {
            assertNotNull(expenseWithUser.getExpense());
            assertNotNull(expenseWithUser.getUser());
            assertNotNull(expenseWithUser.getApproval());
        });

        verify(expenseRepository, times(1)).findAllExpensesWithUsers();
    }

    // Helper method to create ExpenseWithUser objects for testing
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