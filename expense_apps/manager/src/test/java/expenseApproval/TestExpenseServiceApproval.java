package expenseApproval;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Service")
@DisplayName("ExpenseService Approval Tests")
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
    private static String approve;
    private static String deny;

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
        approve = "approved";
        deny = "denied";
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

    // C20_01
    @Story("Expense Approval")
    @Description("Successful expense approval with comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C20_01")
    public void testApproveExpense_normal_returnTrue() {
        actualResult = service.approveExpense(existingExpenseId, existingManagerId, comment);
        assertTrue(actualResult);
        verify(approvalDAO).updateApprovalStatus(existingExpenseId, approve, existingManagerId, comment);
    }

    // C20_02
    @Story("Expense Approval")
    @Description("Successful expense approval with no comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C20_02")
    public void testApproveExpense_noComment_returnTrue() {
        actualResult = service.approveExpense(existingExpenseId, existingManagerId, null);
        assertTrue(actualResult);
        verify(approvalDAO).updateApprovalStatus(existingExpenseId, approve, existingManagerId, null);
    }

    // C20_03
    @Story("Expense Approval")
    @Description("Approving an expense with an invalid expense ID (expense not in database)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C20_03")
    public void testApproveExpense_invalidExpense_returnFalse() {
        actualResult = service.approveExpense(notRealExpenseId, existingManagerId, comment);
        assertFalse(actualResult);
        verify(approvalDAO).updateApprovalStatus(notRealExpenseId, approve, existingManagerId, comment);
    }

    // C20_04
    @Story("Expense Approval")
    @Description("Approving an expense with an invalid manager ID (manager not in database)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C20_04")
    public void testApproveExpense_invalidManager_returnFalse() {
        actualResult = service.approveExpense(existingExpenseId, notRealManagerId, comment);
        assertFalse(actualResult);
        verify(approvalDAO).updateApprovalStatus(existingExpenseId, approve, notRealManagerId, comment);
    }

    // C21_01
    @Story("Expense Denial")
    @Description("Successful expense denial with comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C21_01")
    public void testDenyExpense_normal_returnTrue() {
        actualResult = service.denyExpense(existingExpenseId, existingManagerId, comment);
        assertTrue(actualResult);
        verify(approvalDAO).updateApprovalStatus(existingExpenseId, deny, existingManagerId, comment);
    }

    // C21_02
    @Story("Expense Denial")
    @Description("Successful expense denial with no comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C21_02")
    public void testDenyExpense_noComment_returnTrue() {
        actualResult = service.denyExpense(existingExpenseId, existingManagerId, null);
        assertTrue(actualResult);
        verify(approvalDAO).updateApprovalStatus(existingExpenseId, deny, existingManagerId, null);
    }

    // C21_03
    @Story("Expense Denial")
    @Description("Denying an expense with an invalid expense ID (expense not in database)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C21_03")
    public void testDenyExpense_invalidExpense_returnFalse() {
        actualResult = service.denyExpense(notRealExpenseId, existingManagerId, comment);
        assertFalse(actualResult);
        verify(approvalDAO).updateApprovalStatus(notRealExpenseId, deny, existingManagerId, comment);
    }

    // C21_04
    @Story("Expense Denial")
    @Description("Denying an expense with an invalid manager ID (manager not in database)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C21_04")
    public void testDenyExpense_invalidManager_returnFalse() {
        actualResult = service.denyExpense(existingExpenseId, notRealManagerId, comment);
        assertFalse(actualResult);
        verify(approvalDAO).updateApprovalStatus(existingExpenseId, deny, notRealManagerId, comment);
    }
}
