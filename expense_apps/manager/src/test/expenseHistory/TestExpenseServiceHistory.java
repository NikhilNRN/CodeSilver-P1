package expenseHistory;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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

        existingExpense = new Expense(1, 1, 12.59, "Chick-fil-a for breakfast", "12-12-2025");
        existingUser = new User(1, "employee123", "password123", "Employee");
        existingApproval = new Approval(1, 1, "approved", 2, "You deserve a treat", "12-12-2025");
        existingExpenseWithUser = new ExpenseWithUser(existingExpense, existingUser, existingApproval);
    }

    // TC-1
    @Test
    public void testGetExpensesByAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    // TC-2
    @Test
    public void testGetExpensesByEmployee_existingUser_returnsList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        expectedList.add(existingExpenseWithUser);
        when(expenseDAO.findExpensesByUser(existingUser.getId())).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByEmployee(existingUser.getId());
        assertEquals(expectedList, actualList);
    }

    // TC-3
    @Test
    public void testGetExpensesByEmployee_userNotFound_returnsEmptyList() {
        List<ExpenseWithUser> expectedList = new ArrayList<>();
        when(expenseDAO.findExpensesByUser(99)).thenReturn(expectedList);

        List<ExpenseWithUser> actualList = service.getExpensesByEmployee(99);
        assertEquals(expectedList, actualList);
    }

    //
}
