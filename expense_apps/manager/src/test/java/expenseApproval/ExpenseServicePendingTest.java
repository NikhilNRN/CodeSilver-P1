package expenseApproval;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.format.DateTimeFormatter;

@DisplayName("ExpenseService - getPendingExpenses() Tests")
@Story("User Story 1.1")
class ExpenseServicePendingTest
{

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
    }

    private String formatDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(formatter);
    }

    @Test
    @DisplayName("Should return all pending expenses with user information")
    void testGetPendingExpenses_ReturnsAllPendingExpensesWithUsers()
    {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        // Create first expense with user
        User user1 = new User();
        user1.setId(100);
        user1.setUsername("johndoe");
        user1.setRole("employee");

        Expense expense1 = new Expense();
        expense1.setId(1);
        expense1.setUserId(100);
        expense1.setAmount(150.00);
        expense1.setDescription("Office Supplies");
        expense1.setDate(formatDate(LocalDate.now().minusDays(2)));

        Approval approval1 = new Approval();
        approval1.setId(1);
        approval1.setExpenseId(1);
        approval1.setStatus("pending");

        ExpenseWithUser expenseWithUser1 = new ExpenseWithUser(expense1, user1, approval1);

        // Create second expense with user
        User user2 = new User();
        user2.setId(101);
        user2.setUsername("janesmith");
        user2.setRole("employee");

        Expense expense2 = new Expense();
        expense2.setId(2);
        expense2.setUserId(101);
        expense2.setAmount(200.00);
        expense2.setDescription("Travel Expenses");
        expense2.setDate(formatDate(LocalDate.now().minusDays(1)));

        Approval approval2 = new Approval();
        approval2.setId(2);
        approval2.setExpenseId(2);
        approval2.setStatus("pending");

        ExpenseWithUser expenseWithUser2 = new ExpenseWithUser(expense2, user2, approval2);

        mockExpenses.add(expenseWithUser1);
        mockExpenses.add(expenseWithUser2);

        when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getPendingExpenses();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 pending expenses");

        // Verify first expense has user information
        ExpenseWithUser firstExpense = result.get(0);
        assertNotNull(firstExpense.getUser(), "User should not be null");
        assertNotNull(firstExpense.getExpense(), "Expense should not be null");
        assertNotNull(firstExpense.getApproval(), "Approval should not be null");

        assertEquals("johndoe", firstExpense.getUser().getUsername(), "User name should match");
        assertEquals(100, firstExpense.getUser().getId(), "User ID should match");
        assertEquals("pending", firstExpense.getApproval().getStatus(), "Status should be pending");
        assertEquals(150.00, firstExpense.getExpense().getAmount(), "Amount should match");

        // Verify second expense has user information
        ExpenseWithUser secondExpense = result.get(1);
        assertNotNull(secondExpense.getUser(), "User should not be null");
        assertNotNull(secondExpense.getExpense(), "Expense should not be null");
        assertEquals("janesmith", secondExpense.getUser().getUsername(), "User name should match");
        assertEquals(101, secondExpense.getUser().getId(), "User ID should match");

        verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
    }

    @Test
    @DisplayName("Should return empty list when no pending expenses exist")
    void testGetPendingExpenses_ReturnsEmptyListWhenNoPendingExpenses()
    {
        // Arrange
        List<ExpenseWithUser> emptyList = new ArrayList<>();
        when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(emptyList);

        // Act
        List<ExpenseWithUser> result = expenseService.getPendingExpenses();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be an empty list");
        assertEquals(0, result.size(), "List size should be 0");

        verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
    }

    @Test
    @DisplayName("Should verify correct JOIN operation with users and approvals tables")
    void testGetPendingExpenses_VerifiesCorrectJoinOperation()
    {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        User user = new User();
        user.setId(100);
        user.setUsername("johndoe");
        user.setRole("employee");

        Expense expense = new Expense();
        expense.setId(1);
        expense.setUserId(100);
        expense.setAmount(150.00);
        expense.setDescription("Office Supplies");
        expense.setDate(formatDate(LocalDate.now()));

        Approval approval = new Approval();
        approval.setId(1);
        approval.setExpenseId(1);
        approval.setStatus("pending");
        approval.setReviewer(200);

        ExpenseWithUser expenseWithUser = new ExpenseWithUser(expense, user, approval);
        mockExpenses.add(expenseWithUser);

        when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getPendingExpenses();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");

        ExpenseWithUser resultExpense = result.get(0);

        // Verify that expense data is present (from expenses table)
        assertNotNull(resultExpense.getExpense(), "Expense object should not be null");
        assertTrue(resultExpense.getExpense().getId() > 0, "Expense ID should be populated from expenses table");
        assertTrue(resultExpense.getExpense().getAmount() > 0, "Amount should be populated from expenses table");
        assertNotNull(resultExpense.getExpense().getDescription(), "Description should be populated from expenses table");
        assertEquals(1, resultExpense.getExpense().getId(), "Expense ID should match");

        // Verify that user data is present (from users table JOIN)
        assertNotNull(resultExpense.getUser(), "User object should not be null");
        assertTrue(resultExpense.getUser().getId() > 0, "User ID should be populated from users table");
        assertNotNull(resultExpense.getUser().getUsername(), "User name should be populated from users table JOIN");
        assertEquals(100, resultExpense.getUser().getId(), "User ID should match");

        // Verify that approval data is present (from approvals table JOIN)
        assertNotNull(resultExpense.getApproval(), "Approval object should not be null");
        assertTrue(resultExpense.getApproval().getId() > 0, "Approval ID should be populated from approvals table");
        assertEquals("pending", resultExpense.getApproval().getStatus(), "Status should be pending");
        assertNotNull(resultExpense.getApproval().getReviewer(), "Reviewer should be populated from approvals table");
        assertTrue(resultExpense.getApproval().isPending(), "isPending() should return true");

        verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
    }

    @Test
    @DisplayName("Should verify ordering by date in descending order")
    void testGetPendingExpenses_VerifiesDescendingDateOrder()
    {
        // Arrange
        List<ExpenseWithUser> mockExpenses = new ArrayList<>();

        LocalDate today = LocalDate.now();

        // Most recent expense (should be first)
        User user1 = new User();
        user1.setId(100);
        user1.setUsername("johndoe");

        Expense expense1 = new Expense();
        expense1.setId(1);
        expense1.setUserId(100);
        expense1.setAmount(150.00);
        expense1.setDescription("Recent Expense");
        expense1.setDate(formatDate(today.minusDays(1))); // 1 day ago

        Approval approval1 = new Approval();
        approval1.setId(1);
        approval1.setExpenseId(1);
        approval1.setStatus("pending");

        ExpenseWithUser expenseWithUser1 = new ExpenseWithUser(expense1, user1, approval1);

        // Older expense (should be second)
        User user2 = new User();
        user2.setId(101);
        user2.setUsername("janesmith");

        Expense expense2 = new Expense();
        expense2.setId(2);
        expense2.setUserId(101);
        expense2.setAmount(200.00);
        expense2.setDescription("Older Expense");
        expense2.setDate(formatDate(today.minusDays(5))); // 5 days ago

        Approval approval2 = new Approval();
        approval2.setId(2);
        approval2.setExpenseId(2);
        approval2.setStatus("pending");

        ExpenseWithUser expenseWithUser2 = new ExpenseWithUser(expense2, user2, approval2);

        // Oldest expense (should be third)
        User user3 = new User();
        user3.setId(102);
        user3.setUsername("bobjohnson");

        Expense expense3 = new Expense();
        expense3.setId(3);
        expense3.setUserId(102);
        expense3.setAmount(300.00);
        expense3.setDescription("Oldest Expense");
        expense3.setDate(formatDate(today.minusDays(10))); // 10 days ago

        Approval approval3 = new Approval();
        approval3.setId(3);
        approval3.setExpenseId(3);
        approval3.setStatus("pending");

        ExpenseWithUser expenseWithUser3 = new ExpenseWithUser(expense3, user3, approval3);

        mockExpenses.add(expenseWithUser1);
        mockExpenses.add(expenseWithUser2);
        mockExpenses.add(expenseWithUser3);

        when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockExpenses);

        // Act
        List<ExpenseWithUser> result = expenseService.getPendingExpenses();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should return 3 expenses");

        // Verify descending order (most recent first)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate firstDate = LocalDate.parse(result.get(0).getExpense().getDate(), formatter);
        LocalDate secondDate = LocalDate.parse(result.get(1).getExpense().getDate(), formatter);
        LocalDate thirdDate = LocalDate.parse(result.get(2).getExpense().getDate(), formatter);

        assertTrue(
                firstDate.isAfter(secondDate),
                "First expense should be more recent than second"
        );
        assertTrue(
                secondDate.isAfter(thirdDate),
                "Second expense should be more recent than third"
        );

        // Verify the most recent is first
        assertEquals("Recent Expense", result.get(0).getExpense().getDescription(),
                "Most recent expense should be first");
        assertEquals("Oldest Expense", result.get(2).getExpense().getDescription(),
                "Oldest expense should be last");

        verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
    }
}