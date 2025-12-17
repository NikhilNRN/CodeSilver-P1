package expenseHistory;

import com.revature.api.ExpenseController;
import com.revature.service.ExpenseService;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestExpenseControllerHistory {
    // TODO: Declare mocks and inject mocks
    @Mock
    private static ExpenseService service;

    @InjectMocks
    private static ExpenseController controller;

    // Test vars
    private static final Context ctx = mock(Context.class);

    @BeforeAll
    public static void setUp() {
        // TODO: Set up test vars
    }

    @AfterAll
    public static void tearDown() {
        //
    }

    // C09_03
    @Test
    public void testGetExpensesByEmployee_normal_success() {
        // TODO: Implement
        when(ctx.pathParamAsClass("employeeId", Integer.class).get()).thenReturn(1);
        // Stub service layer return
        // Act - call the controller method
        // Assert that the controller method did not throw any exceptions
        // Verify mocked behavior
    }

    // C09_04
    @Test
    public void testGetExpensesByEmployee_invalidEmployeeIDFormat_throwsException() {
        // TODO: Implement
    }

    // C09_05
    @Test
    public void testGetExpensesByEmployee_serverError_throwsException() {
        // TODO: Implement
    }
}
