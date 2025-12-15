package expenseApproval;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.ExpenseRepository;
import com.revature.service.ExpenseService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;

public class TestExpenseServiceApproval {
    @Mock
    private static ExpenseRepository expenseDAO;
    @Mock
    private static ApprovalRepository approvalDAO;

    @InjectMocks
    private static ExpenseService service;

    // Test vars

    @BeforeAll
    public static void setUp() {
        expenseDAO = mock(ExpenseRepository.class);
        approvalDAO = mock(ApprovalRepository.class);
        service = new ExpenseService(expenseDAO, approvalDAO);

        // Instantiate other static test vars
    }

    @AfterAll
    public static void tearDown() {
        //
    }

    //
    @Test
    public void testApproveExpense_normal_returnTrue() {
        //
    }

    //
    @Test
    public void testApproveExpense_noComment_returnTrue() {
        //
    }

    //
    @Test
    public void testApproveExpense_invalidExpense_returnFalse() {
        //
    }

    //
    @Test
    public void testApproveExpense_invalidManager_returnFalse() {
        //
    }

    //
    @Test
    public void testDenyExpense_normal_returnTrue() {
        //
    }

    //
    @Test
    public void testDenyExpense_noComment_returnTrue() {
        //
    }

    //
    @Test
    public void testDenyExpense_invalidExpense_returnFalse() {
        //
    }

    //
    @Test
    public void testDenyExpense_invalidManager_returnFalse() {
        //
    }
}
