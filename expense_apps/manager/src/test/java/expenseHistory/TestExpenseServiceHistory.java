package expenseHistory;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Service")
@Story("Viewing Expense History")
@DisplayName("ExpenseService History Tests")
public class TestExpenseServiceHistory {

    @Mock
    private static ExpenseRepository expenseDAO;

    @Mock
    private static ApprovalRepository approvalDAO;

    @InjectMocks
    private static ExpenseService service;

    private static Expense existingExpense;
    private static User existingUser;
    private static Approval existingApproval;
    private static ExpenseWithUser existingExpenseWithUser;

    @BeforeAll
    public static void setUp() {
        expenseDAO = mock(ExpenseRepository.class);
        approvalDAO = mock(ApprovalRepository.class);
        service = new ExpenseService(expenseDAO, approvalDAO);

        existingExpense = new Expense(1, 1, 12.59, "Chick-fil-a for breakfast", "2025-12-12");
        existingUser = new User(1, "employee123", "password123", "Employee");
        existingApproval = new Approval(1, 1, "approved", 2, "You deserve a treat", "2025-12-12");
        existingExpenseWithUser = new ExpenseWithUser(existingExpense, existingUser, existingApproval);
    }

    // -----------------------------
    // Step-level helper methods
    // -----------------------------
    @Step("Fetch expenses by employee ID: {employeeId}")
    private List<ExpenseWithUser> fetchExpensesByEmployee(int employeeId) {
        return service.getExpensesByEmployee(employeeId);
    }

    @Step("Fetch expenses by date range: {startDate} to {endDate}")
    private List<ExpenseWithUser> fetchExpensesByDateRange(String startDate, String endDate) {
        return service.getExpensesByDateRange(startDate, endDate);
    }

    // -----------------------------
    // Tests
    // -----------------------------
    @Description("Finding expenses by amount, unimplemented feature that cannot be traced back to a requirement")
    @Severity(SeverityLevel.TRIVIAL)
    @Test
    @Disabled("Feature not yet implemented - CSP-11")
    @DisplayName("C11_01")
    public void testGetExpensesByAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    @Description("Get all expenses from an existing employee by their ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C09_01")
    public void testGetExpensesByEmployee_existingUser_returnsList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        expectedList.add(existingExpenseWithUser);
        when(expenseDAO.findExpensesByUser(existingUser.getId())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = fetchExpensesByEmployee(existingUser.getId());

        assertEquals(expectedList, actualList);
        verify(expenseDAO, times(1)).findExpensesByUser(existingUser.getId());
    }

    @Description("Get all expenses for an unregistered employee ID")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C09_02")
    public void testGetExpensesByEmployee_userNotFound_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByUser(99)).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = fetchExpensesByEmployee(99);

        assertEquals(expectedList, actualList);
        verify(expenseDAO, times(1)).findExpensesByUser(99);
    }

    @Description("Get all expenses within a valid date range")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C10_01")
    public void testGetExpensesByDateRange_normalRange_returnsList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        expectedList.add(existingExpenseWithUser);
        expectedList.add(existingExpenseWithUser);
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = fetchExpensesByDateRange("2025-12-11", "2025-12-13");

        assertEquals(expectedList, actualList);
        verify(expenseDAO, times(1)).findExpensesByDateRange("2025-12-11", "2025-12-13");
    }

    @Description("Get all expenses within an invalid date range (empty list expected)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C10_02")
    public void testGetExpensesByDateRange_invalidRange_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = fetchExpensesByDateRange("2025-12-13", "2025-12-11");

        assertEquals(expectedList, actualList);
        verify(expenseDAO, times(1)).findExpensesByDateRange("2025-12-13", "2025-12-11");
    }

    @Description("Get expenses for an improperly formatted date range (empty list expected)")
    @Test
    @DisplayName("C10_03")
    public void testGetExpensesByDateRange_invalidFormat_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = fetchExpensesByDateRange("12-13-2025", "12-11-2025");

        assertEquals(expectedList, actualList);
        verify(expenseDAO, times(1)).findExpensesByDateRange("12-13-2025", "12-11-2025");
    }

    @Description("Get expenses for date range (null dates, empty list expected)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C10_04")
    public void testGetExpensesByDateRange_nullDates_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = fetchExpensesByDateRange(null, null);

        assertEquals(expectedList, actualList);
        verify(expenseDAO, times(1)).findExpensesByDateRange(null, null);
    }

    // Disabled sorting tests
    @Description("Sorting expenses by newest amount, unimplemented feature")
    @Severity(SeverityLevel.TRIVIAL)
    @Test
    @Disabled("Feature not yet implemented - CSP-12")
    @DisplayName("C12_01")
    public void testSortExpensesByNewestAmount_expectedFail() { fail("Unimplemented"); }

    @Description("Sorting expenses by oldest amount, unimplemented feature")
    @Severity(SeverityLevel.TRIVIAL)
    @Test
    @Disabled("Feature not yet implemented - CSP-13")
    @DisplayName("C13_01")
    public void testSortExpensesByOldestAmount_expectedFail() { fail("Unimplemented"); }

    @Description("Sorting expenses by highest amount, unimplemented feature")
    @Severity(SeverityLevel.TRIVIAL)
    @Test
    @Disabled("Feature not yet implemented - CSP-14")
    @DisplayName("C14_01")
    public void testSortExpensesByHighestAmount_expectedFail() { fail("Unimplemented"); }

    @Description("Sorting expenses by lowest amount, unimplemented feature")
    @Severity(SeverityLevel.TRIVIAL)
    @Test
    @Disabled("Feature not yet implemented - CSP-15")
    @DisplayName("C15_01")
    public void testSortExpensesByLowestAmount_expectedFail() { fail("Unimplemented"); }
}
