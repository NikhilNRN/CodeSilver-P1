package expenseApproval;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Service")
@DisplayName("ExpenseService Approval Tests")
public class TestExpenseServiceApproval {

    @Mock
    private ExpenseRepository expenseDAO;
    @Mock
    private ApprovalRepository approvalDAO;

    @InjectMocks
    private ExpenseService service;

    // Test variables
    private int existingExpenseId;
    private int notRealExpenseId;
    private int existingManagerId;
    private int notRealManagerId;
    private String comment;
    private String approve;
    private String deny;

    @BeforeEach
    public void setUp() {
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

        // Stub approvalDAO for various scenarios
        when(approvalDAO.updateApprovalStatus(existingExpenseId, approve, existingManagerId, comment)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, approve, existingManagerId, null)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, deny, existingManagerId, comment)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, deny, existingManagerId, null)).thenReturn(true);
        when(approvalDAO.updateApprovalStatus(notRealExpenseId, approve, existingManagerId, comment)).thenReturn(false);
        when(approvalDAO.updateApprovalStatus(notRealExpenseId, deny, existingManagerId, comment)).thenReturn(false);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, approve, notRealManagerId, comment)).thenReturn(false);
        when(approvalDAO.updateApprovalStatus(existingExpenseId, deny, notRealManagerId, comment)).thenReturn(false);
    }

    // C20_01
    @Story("Expense Approval")
    @Description("Successful expense approval with comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C20_01")
    public void testApproveExpense_normal_returnTrue() {
        Allure.step("Call approveExpense with existing expense and manager IDs", () -> {
            boolean result = service.approveExpense(existingExpenseId, existingManagerId, comment);

            Allure.step("Assert result is true", () -> assertTrue(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(existingExpenseId, approve, existingManagerId, comment)
            );
        });
    }

    // C20_02
    @Story("Expense Approval")
    @Description("Successful expense approval with no comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C20_02")
    public void testApproveExpense_noComment_returnTrue() {
        Allure.step("Call approveExpense with null comment", () -> {
            boolean result = service.approveExpense(existingExpenseId, existingManagerId, null);

            Allure.step("Assert result is true", () -> assertTrue(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(existingExpenseId, approve, existingManagerId, null)
            );
        });
    }

    // C20_03
    @Story("Expense Approval")
    @Description("Approving an expense with an invalid expense ID (expense not in database)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C20_03")
    public void testApproveExpense_invalidExpense_returnFalse() {
        Allure.step("Call approveExpense with invalid expense ID", () -> {
            boolean result = service.approveExpense(notRealExpenseId, existingManagerId, comment);

            Allure.step("Assert result is false", () -> assertFalse(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(notRealExpenseId, approve, existingManagerId, comment)
            );
        });
    }

    // C20_04
    @Story("Expense Approval")
    @Description("Approving an expense with an invalid manager ID (manager not in database)")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C20_04")
    public void testApproveExpense_invalidManager_returnFalse() {
        Allure.step("Call approveExpense with invalid manager ID", () -> {
            boolean result = service.approveExpense(existingExpenseId, notRealManagerId, comment);

            Allure.step("Assert result is false", () -> assertFalse(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(existingExpenseId, approve, notRealManagerId, comment)
            );
        });
    }

    // C21_01
    @Story("Expense Denial")
    @Description("Successful expense denial with comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C21_01")
    public void testDenyExpense_normal_returnTrue() {
        Allure.step("Call denyExpense with comment", () -> {
            boolean result = service.denyExpense(existingExpenseId, existingManagerId, comment);

            Allure.step("Assert result is true", () -> assertTrue(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(existingExpenseId, deny, existingManagerId, comment)
            );
        });
    }

    // C21_02
    @Story("Expense Denial")
    @Description("Successful expense denial with no comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C21_02")
    public void testDenyExpense_noComment_returnTrue() {
        Allure.step("Call denyExpense with null comment", () -> {
            boolean result = service.denyExpense(existingExpenseId, existingManagerId, null);

            Allure.step("Assert result is true", () -> assertTrue(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(existingExpenseId, deny, existingManagerId, null)
            );
        });
    }

    // C21_03
    @Story("Expense Denial")
    @Description("Denying an expense with an invalid expense ID")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C21_03")
    public void testDenyExpense_invalidExpense_returnFalse() {
        Allure.step("Call denyExpense with invalid expense ID", () -> {
            boolean result = service.denyExpense(notRealExpenseId, existingManagerId, comment);

            Allure.step("Assert result is false", () -> assertFalse(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(notRealExpenseId, deny, existingManagerId, comment)
            );
        });
    }

    // C21_04
    @Story("Expense Denial")
    @Description("Denying an expense with an invalid manager ID")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C21_04")
    public void testDenyExpense_invalidManager_returnFalse() {
        Allure.step("Call denyExpense with invalid manager ID", () -> {
            boolean result = service.denyExpense(existingExpenseId, notRealManagerId, comment);

            Allure.step("Assert result is false", () -> assertFalse(result));
            Allure.step("Verify DAO updateApprovalStatus called", () ->
                    verify(approvalDAO).updateApprovalStatus(existingExpenseId, deny, notRealManagerId, comment)
            );
        });
    }
}
