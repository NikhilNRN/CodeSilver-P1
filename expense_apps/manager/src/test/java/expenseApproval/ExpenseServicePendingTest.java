package expenseApproval;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Epic("Expense Management")
@Feature("Retrieve Pending Expenses")
@DisplayName("ExpenseService - getPendingExpenses() Tests")
class ExpenseServicePendingTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(formatter);
    }

    @Test
    @DisplayName("Should return all pending expenses with user information")
    @Story("Retrieve all pending expenses with user info")
    @Severity(SeverityLevel.CRITICAL)
    void testGetPendingExpenses_ReturnsAllPendingExpensesWithUsers() {
        Allure.step("Arrange: prepare mock pending expenses with user info", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();

            User user1 = new User();
            user1.setId(100); user1.setUsername("johndoe"); user1.setRole("employee");
            Expense expense1 = new Expense(); expense1.setId(1); expense1.setUserId(100);
            expense1.setAmount(150.00); expense1.setDescription("Office Supplies");
            expense1.setDate(formatDate(LocalDate.now().minusDays(2)));
            Approval approval1 = new Approval(); approval1.setId(1); approval1.setExpenseId(1); approval1.setStatus("pending");
            mockExpenses.add(new ExpenseWithUser(expense1, user1, approval1));

            User user2 = new User();
            user2.setId(101); user2.setUsername("janesmith"); user2.setRole("employee");
            Expense expense2 = new Expense(); expense2.setId(2); expense2.setUserId(101);
            expense2.setAmount(200.00); expense2.setDescription("Travel Expenses");
            expense2.setDate(formatDate(LocalDate.now().minusDays(1)));
            Approval approval2 = new Approval(); approval2.setId(2); approval2.setExpenseId(2); approval2.setStatus("pending");
            mockExpenses.add(new ExpenseWithUser(expense2, user2, approval2));

            when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockExpenses);

            Allure.step("Act: call getPendingExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getPendingExpenses();

                Allure.step("Assert: verify expenses and user information", () -> {
                    assertNotNull(result, "Result should not be null");
                    assertEquals(2, result.size(), "Should return 2 pending expenses");

                    ExpenseWithUser firstExpense = result.get(0);
                    assertNotNull(firstExpense.getUser()); assertNotNull(firstExpense.getExpense()); assertNotNull(firstExpense.getApproval());
                    assertEquals("johndoe", firstExpense.getUser().getUsername());
                    assertEquals(100, firstExpense.getUser().getId());
                    assertEquals("pending", firstExpense.getApproval().getStatus());
                    assertEquals(150.00, firstExpense.getExpense().getAmount());

                    ExpenseWithUser secondExpense = result.get(1);
                    assertEquals("janesmith", secondExpense.getUser().getUsername());
                    assertEquals(101, secondExpense.getUser().getId());

                    verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
                });
            });
        });
    }

    @Test
    @DisplayName("Should return empty list when no pending expenses exist")
    @Story("Handle empty pending expense list")
    @Severity(SeverityLevel.MINOR)
    void testGetPendingExpenses_ReturnsEmptyListWhenNoPendingExpenses() {
        Allure.step("Arrange: repository returns empty list", () -> {
            when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(new ArrayList<>());

            Allure.step("Act: call getPendingExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getPendingExpenses();

                Allure.step("Assert: result should be empty", () -> {
                    assertNotNull(result);
                    assertTrue(result.isEmpty());
                    assertEquals(0, result.size());
                    verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
                });
            });
        });
    }

    @Test
    @DisplayName("Should verify correct JOIN operation with users and approvals tables")
    @Story("Verify pending expenses JOIN with users and approvals")
    @Severity(SeverityLevel.NORMAL)
    void testGetPendingExpenses_VerifiesCorrectJoinOperation() {
        Allure.step("Arrange: prepare one pending expense with complete join data", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();

            User user = new User(); user.setId(100); user.setUsername("johndoe"); user.setRole("employee");
            Expense expense = new Expense(); expense.setId(1); expense.setUserId(100); expense.setAmount(150.00);
            expense.setDescription("Office Supplies"); expense.setDate(formatDate(LocalDate.now()));
            Approval approval = new Approval(); approval.setId(1); approval.setExpenseId(1); approval.setStatus("pending"); approval.setReviewer(200);
            mockExpenses.add(new ExpenseWithUser(expense, user, approval));

            when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockExpenses);

            Allure.step("Act: call getPendingExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getPendingExpenses();

                Allure.step("Assert: verify expense, user, and approval data", () -> {
                    assertNotNull(result); assertFalse(result.isEmpty());
                    ExpenseWithUser resultExpense = result.get(0);

                    assertNotNull(resultExpense.getExpense());
                    assertTrue(resultExpense.getExpense().getId() > 0);
                    assertTrue(resultExpense.getExpense().getAmount() > 0);
                    assertNotNull(resultExpense.getExpense().getDescription());
                    assertEquals(1, resultExpense.getExpense().getId());

                    assertNotNull(resultExpense.getUser());
                    assertEquals(100, resultExpense.getUser().getId());
                    assertNotNull(resultExpense.getUser().getUsername());

                    assertNotNull(resultExpense.getApproval());
                    assertEquals("pending", resultExpense.getApproval().getStatus());
                    assertNotNull(resultExpense.getApproval().getReviewer());
                    assertTrue(resultExpense.getApproval().isPending());

                    verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
                });
            });
        });
    }

    @Test
    @DisplayName("Should verify ordering by date in descending order")
    @Story("Verify pending expenses sorted by most recent date")
    @Severity(SeverityLevel.NORMAL)
    void testGetPendingExpenses_VerifiesDescendingDateOrder() {
        Allure.step("Arrange: prepare pending expenses with different dates", () -> {
            List<ExpenseWithUser> mockExpenses = new ArrayList<>();
            LocalDate today = LocalDate.now();

            User user1 = new User(); user1.setId(100); user1.setUsername("johndoe");
            Expense expense1 = new Expense(); expense1.setId(1); expense1.setUserId(100); expense1.setAmount(150.00);
            expense1.setDescription("Recent Expense"); expense1.setDate(formatDate(today.minusDays(1)));
            Approval approval1 = new Approval(); approval1.setId(1); approval1.setExpenseId(1); approval1.setStatus("pending");
            mockExpenses.add(new ExpenseWithUser(expense1, user1, approval1));

            User user2 = new User(); user2.setId(101); user2.setUsername("janesmith");
            Expense expense2 = new Expense(); expense2.setId(2); expense2.setUserId(101); expense2.setAmount(200.00);
            expense2.setDescription("Older Expense"); expense2.setDate(formatDate(today.minusDays(5)));
            Approval approval2 = new Approval(); approval2.setId(2); approval2.setExpenseId(2); approval2.setStatus("pending");
            mockExpenses.add(new ExpenseWithUser(expense2, user2, approval2));

            User user3 = new User(); user3.setId(102); user3.setUsername("bobjohnson");
            Expense expense3 = new Expense(); expense3.setId(3); expense3.setUserId(102); expense3.setAmount(300.00);
            expense3.setDescription("Oldest Expense"); expense3.setDate(formatDate(today.minusDays(10)));
            Approval approval3 = new Approval(); approval3.setId(3); approval3.setExpenseId(3); approval3.setStatus("pending");
            mockExpenses.add(new ExpenseWithUser(expense3, user3, approval3));

            when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockExpenses);

            Allure.step("Act: call getPendingExpenses()", () -> {
                List<ExpenseWithUser> result = expenseService.getPendingExpenses();

                Allure.step("Assert: verify descending date order", () -> {
                    assertNotNull(result);
                    assertEquals(3, result.size());

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDate firstDate = LocalDate.parse(result.get(0).getExpense().getDate(), formatter);
                    LocalDate secondDate = LocalDate.parse(result.get(1).getExpense().getDate(), formatter);
                    LocalDate thirdDate = LocalDate.parse(result.get(2).getExpense().getDate(), formatter);

                    assertTrue(firstDate.isAfter(secondDate), "First expense should be more recent than second");
                    assertTrue(secondDate.isAfter(thirdDate), "Second expense should be more recent than third");

                    assertEquals("Recent Expense", result.get(0).getExpense().getDescription());
                    assertEquals("Oldest Expense", result.get(2).getExpense().getDescription());

                    verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
                });
            });
        });
    }
}
