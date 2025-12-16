package expenseApproval;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class TestExpenseServiceApproval {
    @Mock
    private static ExpenseRepository expenseDAO;
    @Mock
    private static ApprovalRepository approvalDAO;

    @InjectMocks
    private static ExpenseService service;

    // Test vars
    private static int existingExpenseId;
    private static int notRealExpenseId;
    private static int existingManagerId;
    private static int notRealManagerId;
    private static String comment;

    private boolean actualResult;

    @BeforeAll
    public static void setUp() {
        expenseDAO = mock(ExpenseRepository.class);
        approvalDAO = mock(ApprovalRepository.class);
        service = new ExpenseService(expenseDAO, approvalDAO);

        existingExpenseId = 4;
        existingManagerId = 1;
        notRealExpenseId = 999;
        notRealManagerId = -5;
        String approve = "approved";
        String deny = "denied";
        comment = "Some words";

        when(approvalDAO.updateApprovalStatus(existingExpenseId, approve, existingManagerId, comment)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, approve, existingManagerId, null)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, deny, existingManagerId, comment)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, deny, existingManagerId, null)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(notRealExpenseId, approve, existingExpenseId, comment)).thenReturn(false);
        when(approvalDAO.updateApprovalStatus(notRealExpenseId, deny, existingExpenseId, comment)).thenReturn(false);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, approve, notRealManagerId, comment)).thenReturn(false);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, deny, notRealManagerId, comment)).thenReturn(false);
    }

    @AfterAll
    public static void tearDown() {
        //
    }

    // TC-C20_01
    @Test
    public void testApproveExpense_normal_returnTrue() {
        actualResult = service.approveExpense(existingExpenseId, existingManagerId, comment);
        assertTrue(actualResult);
    }

    // TC-C20_02
    @Test
    public void testApproveExpense_noComment_returnTrue() {
        actualResult = service.approveExpense(existingExpenseId, existingManagerId, null);
        assertTrue(actualResult);
    }

    // TC-C20_03
    @Test
    public void testApproveExpense_invalidExpense_returnFalse() {
        actualResult = service.approveExpense(notRealExpenseId, existingManagerId, comment);
        assertFalse(actualResult);
    }

    // TC-C20_04
    @Test
    public void testApproveExpense_invalidManager_returnFalse() {
        actualResult = service.approveExpense(existingExpenseId, notRealManagerId, comment);
        assertFalse(actualResult);
    }

    // TC-C21_01
    @Test
    public void testDenyExpense_normal_returnTrue() {
        actualResult = service.denyExpense(existingExpenseId, existingManagerId, comment);
        assertTrue(actualResult);
    }

    // TC-C21_02
    @Test
    public void testDenyExpense_noComment_returnTrue() {
        actualResult = service.denyExpense(existingExpenseId, existingManagerId, null);
        assertTrue(actualResult);
    }

    // TC-C21_03
    @Test
    public void testDenyExpense_invalidExpense_returnFalse() {
        actualResult = service.denyExpense(notRealExpenseId, existingManagerId, comment);
        assertFalse(actualResult);
    }

    // TC-C21_04
    @Test
    public void testDenyExpense_invalidManager_returnFalse() {
        actualResult = service.denyExpense(existingExpenseId, notRealManagerId, comment);
        assertFalse(actualResult);
    }
}
