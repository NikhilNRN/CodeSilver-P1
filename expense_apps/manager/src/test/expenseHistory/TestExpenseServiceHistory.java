package expenseHistory;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // TC-C11_01
    @Test
    @Disabled("Feature not yet implemented - CSP-11")
    public void testGetExpensesByAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    // C09_01
    @Test
    public void testGetExpensesByEmployee_existingUser_returnsList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        expectedList.add(existingExpenseWithUser);
        when(expenseDAO.findExpensesByUser(existingUser.getId())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByEmployee(existingUser.getId());
        assertEquals(expectedList, actualList);
    }

    // C09_02
    @Test
    public void testGetExpensesByEmployee_userNotFound_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByUser(99)).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByEmployee(99);
        assertEquals(expectedList, actualList);
    }

    // C10_01
    @Test
    public void testGetExpensesByDateRange_normalRange_returnsList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        expectedList.add(existingExpenseWithUser);
        expectedList.add(existingExpenseWithUser);
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByDateRange("2025-12-11", "2025-12-13");
        assertEquals(expectedList, actualList);
    }

    // C10_02
    @Test
    public void testGetExpensesByDateRange_invalidRange_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByDateRange("2025-12-13", "2025-12-11");
        assertEquals(expectedList, actualList);
    }

    // C10_03
    @Test
    public void testGetExpensesByDateRange_invalidFormat_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByDateRange("12-13-2025", "12-11-2025");
        assertEquals(expectedList, actualList);
    }

    // C10_04
    @Test
    public void testGetExpensesByDateRange_nullDates_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByDateRange(anyString(), anyString())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByDateRange(null, null);
        assertEquals(expectedList, actualList);
    }

    // C12_01
    @Test
    @Disabled("Feature not yet implemented - CSP-12")
    public void testSortExpensesByNewestAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    // C13_01
    @Test
    @Disabled("Feature not yet implemented - CSP-13")
    public void testSortExpensesByOldestAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    // C14_01
    @Test
    @Disabled("Feature not yet implemented - CSP-14")
    public void testSortExpensesByHighestAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    // C15_01
    @Test
    @Disabled("Feature not yet implemented - CSP-15")
    public void testSortExpensesByLowestAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }
}
